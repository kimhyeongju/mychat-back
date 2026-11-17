package com.khj.playground.invest.service;

import com.khj.playground.invest.dto.SplitAdjustmentResponse;
import com.khj.playground.invest.entity.Holding;
import com.khj.playground.invest.entity.InvestAccount;
import com.khj.playground.invest.entity.SplitAdjustment;
import com.khj.playground.invest.entity.StockSplit;
import com.khj.playground.invest.market.MarketCalendar;
import com.khj.playground.invest.market.MarketDataClient;
import com.khj.playground.invest.market.MarketDataException;
import com.khj.playground.invest.market.Quote;
import com.khj.playground.invest.market.StockSplitInfo;
import com.khj.playground.invest.repository.HoldingRepository;
import com.khj.playground.invest.repository.InvestAccountRepository;
import com.khj.playground.invest.repository.SplitAdjustmentRepository;
import com.khj.playground.invest.repository.StockSplitRepository;
import com.khj.playground.invest.repository.TradeRepository;
import com.khj.playground.invest.repository.WatchlistRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * 주식 분할/병합을 보유 수량과 평균 단가에 반영한다.
 *
 * 시세는 분할 후 가격으로 바뀌는데 수량이 그대로면 수익률이 틀어진다. 그래서
 *  1) 시세 제공자의 기업 이벤트에서 일정을 받아 두고(sync),
 *  2) ex-date 가 지난 뒤 "시세가 실제로 분할 후 가격이 된 것"을 확인한 순간에 수량·단가를 조정한다(applyDue).
 *
 * 조정 시점을 시세에 맞추는 이유: 시세는 분할 후, 수량은 분할 전인 상태가 생기지 않게 하기 위해서다.
 * 시세가 분할 전 가격인 동안에는 수량도 분할 전 그대로 두므로 평가액은 계속 일관된다.
 * 분할 후 시세가 보이는데 아직 조정되지 않은 짧은 구간에는 그 종목 주문을 막는다(assertTradable).
 *
 * 병합으로 생기는 소수 주는 분할 후 시세로 현금 정산한다. 매수 원가는 그대로 유지된다.
 * 거래 내역(invest_trades)은 수정하지 않고, 조정 내용은 invest_split_adjustments 에 남긴다.
 * 단일 인스턴스 전제이며, 같은 분할을 두 번 적용하지 않도록 행 잠금과 applied_at 을 함께 쓴다.
 */
@Slf4j
@Service
public class StockSplitService {

  private static final java.time.ZoneId NEW_YORK = MarketCalendar.NEW_YORK;
  /** ex-date 에 분할 후 첫 체결이 나올 수 있는 가장 이른 시각(프리마켓 시작). */
  private static final LocalTime FIRST_POSSIBLE_TRADE = LocalTime.of(4, 0);

  private final StockSplitRepository splitRepository;
  private final SplitAdjustmentRepository adjustmentRepository;
  private final HoldingRepository holdingRepository;
  private final InvestAccountRepository accountRepository;
  private final TradeRepository tradeRepository;
  private final WatchlistRepository watchlistRepository;
  private final PriceService priceService;
  private final MarketDataClient marketDataClient;
  private final RankingService rankingService;
  private final WeeklyPeriod weeklyPeriod;
  private final TransactionTemplate tx;

  public StockSplitService(
    StockSplitRepository splitRepository,
    SplitAdjustmentRepository adjustmentRepository,
    HoldingRepository holdingRepository,
    InvestAccountRepository accountRepository,
    TradeRepository tradeRepository,
    WatchlistRepository watchlistRepository,
    PriceService priceService,
    MarketDataClient marketDataClient,
    RankingService rankingService,
    WeeklyPeriod weeklyPeriod,
    PlatformTransactionManager transactionManager
  ) {
    this.splitRepository = splitRepository;
    this.adjustmentRepository = adjustmentRepository;
    this.holdingRepository = holdingRepository;
    this.accountRepository = accountRepository;
    this.tradeRepository = tradeRepository;
    this.watchlistRepository = watchlistRepository;
    this.priceService = priceService;
    this.marketDataClient = marketDataClient;
    this.rankingService = rankingService;
    this.weeklyPeriod = weeklyPeriod;
    this.tx = new TransactionTemplate(transactionManager);
  }

  // ---------- 일정 동기화 ----------

  /**
   * 보유·관심·최근 거래 종목의 분할/병합 일정을 받아 저장한다.
   * 지난 3일(서버가 꺼져 있던 동안 놓친 것)부터 30일 뒤까지를 본다.
   *
   * @return 새로 저장한 건수
   */
  public int sync() {
    Set<String> symbols = new LinkedHashSet<>(holdingRepository.findDistinctSymbols());
    symbols.addAll(watchlistRepository.findDistinctSymbols());
    symbols.addAll(
      tradeRepository.findSymbolsTradedSince(LocalDateTime.now().minusDays(7))
    );
    if (symbols.isEmpty()) {
      return 0;
    }

    LocalDate today = LocalDate.now(NEW_YORK);
    List<StockSplitInfo> infos;
    try {
      infos = marketDataClient.fetchSplits(symbols, today.minusDays(3), today.plusDays(30));
    } catch (MarketDataException e) {
      log.warn("분할/병합 일정 조회 실패: {}", e.getMessage());
      return 0;
    }

    Instant now = Instant.now();
    int saved = 0;
    for (StockSplitInfo info : infos) {
      if (splitRepository.existsBySymbolAndExDate(info.symbol(), info.exDate())) {
        continue;
      }
      boolean late = !now.isBefore(firstPossibleTrade(info.exDate()));
      try {
        splitRepository.save(
          StockSplit.of(info.symbol(), info.exDate(), info.newRate(), info.oldRate(), late)
        );
        saved++;
        log.info(
          "분할/병합 일정 등록 {} {} {}:{} ex-date={} late={}",
          info.symbol(),
          info.newRate().compareTo(info.oldRate()) > 0 ? "분할" : "병합",
          info.newRate().stripTrailingZeros().toPlainString(),
          info.oldRate().stripTrailingZeros().toPlainString(),
          info.exDate(),
          late
        );
      } catch (org.springframework.dao.DataIntegrityViolationException e) {
        // 동시에 같은 일정이 들어온 경우. 이미 있으므로 무시한다.
      }
    }
    return saved;
  }

  // ---------- 적용 ----------

  /**
   * ex-date 가 지난 미적용 분할을 확인해, 시세가 분할 후 가격이 된 것부터 적용한다.
   *
   * @return 이번에 적용한 건수
   */
  public int applyDue() {
    LocalDate today = LocalDate.now(NEW_YORK);
    List<StockSplit> due =
      splitRepository.findByAppliedAtIsNullAndExDateLessThanEqualOrderByExDateAsc(today);
    if (due.isEmpty()) {
      return 0;
    }

    refreshQuotes(due.stream().map(StockSplit::getSymbol).distinct().toList());

    int applied = 0;
    for (StockSplit split : due) {
      try {
        if (applyOne(split.getId())) {
          applied++;
        }
      } catch (Exception e) {
        log.error("분할/병합 적용 실패 symbol={} exDate={}", split.getSymbol(), split.getExDate(), e);
      }
    }
    if (applied > 0) {
      rankingService.invalidate();
    }
    return applied;
  }

  private boolean applyOne(UUID splitId) {
    StockSplit split = splitRepository.findById(splitId).orElse(null);
    if (split == null || split.isApplied()) {
      return false;
    }

    Optional<Quote> quote = priceService
      .findQuoteForValuation(split.getSymbol())
      .map(PriceService.QuoteView::quote);
    if (quote.isEmpty() || !isPostSplit(quote.get(), split)) {
      // 아직 분할 전 시세다. 수량도 그대로 둔다.
      return false;
    }

    BigDecimal price = quote.get().price();
    Boolean done = tx.execute(status -> doApply(splitId, price));
    return Boolean.TRUE.equals(done);
  }

  private boolean doApply(UUID splitId, BigDecimal postSplitPrice) {
    StockSplit split = splitRepository.findByIdForUpdate(splitId).orElse(null);
    if (split == null || split.isApplied()) {
      return false;
    }

    String symbol = split.getSymbol();
    LocalDate weekEnd = weeklyPeriod.currentWeekEnd();
    LocalDateTime cutoff = LocalDateTime.ofInstant(
      firstPossibleTrade(split.getExDate()),
      ZoneId.systemDefault()
    );

    int adjusted = 0;
    int skipped = 0;
    // 계좌 행 잠금은 주문과 같은 잠금이다. 항상 id 순으로 잠가 교착을 피한다.
    for (UUID accountId : holdingRepository.findAccountIdsBySymbol(symbol)) {
      InvestAccount account = accountRepository.lockById(accountId).orElse(null);
      Holding holding = holdingRepository.findByAccountIdAndSymbol(accountId, symbol).orElse(null);
      if (account == null || holding == null) {
        continue;
      }
      long before = holding.getQuantity();
      BigDecimal avgBefore = holding.getAvgCost();

      // 분할을 ex-date 이후에야 알게 된 경우, 그 사이 이 종목을 거래한 계좌는 보유분에
      // 분할 전·후 가격의 매수가 섞여 있을 수 있어 자동 조정하지 않는다.
      if (
        split.isLateDiscovery() &&
        tradeRepository.existsByAccountIdAndSymbolAndCreatedAtGreaterThanEqual(
          accountId,
          symbol,
          cutoff
        )
      ) {
        adjustmentRepository.save(
          SplitAdjustment.of(
            split, account, before, before, avgBefore, avgBefore, BigDecimal.ZERO, true, weekEnd
          )
        );
        log.warn(
          "늦게 알게 된 분할이라 조정하지 않음 accountId={} symbol={}",
          accountId,
          symbol
        );
        skipped++;
        continue;
      }

      SplitMath.Result r = SplitMath.adjust(
        before,
        avgBefore,
        split.getNewRate(),
        split.getOldRate()
      );
      BigDecimal cash = r.cashInLieu(postSplitPrice);
      if (r.newQuantity() == 0) {
        holdingRepository.delete(holding);
      } else {
        holding.applySplit(r.newQuantity(), r.newAvgCost());
      }
      if (cash.signum() > 0) {
        account.depositCash(cash);
      }
      adjustmentRepository.save(
        SplitAdjustment.of(
          split, account, before, r.newQuantity(), avgBefore, r.newAvgCost(), cash, false, weekEnd
        )
      );
      adjusted++;
    }

    split.markApplied();
    log.info(
      "분할/병합 적용 {} {}:{} ex-date={} 조정 계좌={} 건너뜀={}",
      symbol,
      split.getNewRate().stripTrailingZeros().toPlainString(),
      split.getOldRate().stripTrailingZeros().toPlainString(),
      split.getExDate(),
      adjusted,
      skipped
    );
    return true;
  }

  private void refreshQuotes(List<String> symbols) {
    try {
      if (priceService.supportsBatch()) {
        priceService.refreshBatch(symbols);
      } else {
        symbols.forEach(priceService::refreshIfStale);
      }
    } catch (Exception e) {
      log.warn("분할 확인용 시세 갱신 실패: {}", e.getMessage());
    }
  }

  // ---------- 주문 보호 ----------

  /**
   * 시세는 이미 분할 후 가격인데 보유 수량은 아직 조정되기 전이면 체결하면 안 된다.
   * (예: 분할 후 가격으로 산 주식이 곧 다시 분할 비율로 곱해진다.)
   * 보통 수십 초 안에 풀린다.
   */
  public void assertTradable(String symbol, Quote quote) {
    LocalDate today = LocalDate.now(NEW_YORK);
    for (StockSplit split :
      splitRepository.findBySymbolAndAppliedAtIsNullAndExDateLessThanEqual(symbol, today)) {
      if (isPostSplit(quote, split)) {
        throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "주식 분할/병합을 반영하는 중입니다. 잠시 후 다시 시도해주세요."
        );
      }
    }
  }

  // ---------- 조회 ----------

  /** 이번 주에 내 계좌에 적용된 조정 내역. 계좌가 없으면 빈 목록이다. */
  @Transactional(readOnly = true)
  public List<SplitAdjustmentResponse> thisWeek(UUID userId) {
    return accountRepository
      .findByUserId(userId)
      .map(account ->
        adjustmentRepository
          .findByAccountIdAndWeekEndDateOrderByCreatedAtDesc(
            account.getId(),
            weeklyPeriod.currentWeekEnd()
          )
          .stream()
          .map(SplitAdjustmentResponse::from)
          .toList()
      )
      .orElse(List.of());
  }

  // ---------- 공통 ----------

  /** 마지막 체결이 ex-date 의 첫 가능 체결 시각 이후면 분할 후 가격이다. 체결 시각을 모르면 판단하지 않는다. */
  static boolean isPostSplit(Quote quote, StockSplit split) {
    return (
      quote.tradedAt() != null &&
      !quote.tradedAt().isBefore(firstPossibleTrade(split.getExDate()))
    );
  }

  private static Instant firstPossibleTrade(LocalDate exDate) {
    return exDate.atTime(FIRST_POSSIBLE_TRADE).atZone(NEW_YORK).toInstant();
  }
}
