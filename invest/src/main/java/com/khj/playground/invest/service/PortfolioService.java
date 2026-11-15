package com.khj.playground.invest.service;

import com.khj.playground.invest.dto.AccountSummaryResponse;
import com.khj.playground.invest.dto.PortfolioResponse;
import com.khj.playground.invest.dto.PositionResponse;
import com.khj.playground.invest.entity.Holding;
import com.khj.playground.invest.entity.InvestAccount;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.MemberLookup;
import com.khj.playground.invest.dto.PublicPortfolioResponse;
import com.khj.playground.invest.dto.PublicPositionResponse;
import com.khj.playground.invest.dto.RankingEntry;
import com.khj.playground.invest.repository.HoldingRepository;
import com.khj.playground.invest.repository.InvestAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 보유 종목을 현재 시세로 평가한다. 시세가 없으면 평균단가로 대신하고 stale 로 표시한다. */
@Service
@RequiredArgsConstructor
public class PortfolioService {

  private final InvestAccountService accountService;
  private final HoldingRepository holdingRepository;
  private final PriceService priceService;
  private final SymbolService symbolService;
  private final InvestAccountRepository accountRepository;
  private final RankingService rankingService;
  private final MemberLookup memberLookup;

  @Transactional
  public PortfolioResponse getPortfolio(UUID userId) {
    InvestAccount account = accountService.getOrOpen(userId);
    List<Holding> holdings = holdingRepository.findByAccountIdOrderBySymbolAsc(
      account.getId()
    );

    List<PositionResponse> positions = new ArrayList<>();
    BigDecimal holdingsValue = BigDecimal.ZERO;
    boolean anyStale = false;

    for (Holding h : holdings) {
      Optional<PriceService.QuoteView> view = priceService.findQuoteForValuation(h.getSymbol());
      BigDecimal price = view.map(v -> v.quote().price()).orElse(null);
      boolean stale = view.map(PriceService.QuoteView::stale).orElse(true);
      anyStale |= stale;

      BigDecimal effective = price != null ? price : h.getAvgCost();
      BigDecimal qty = BigDecimal.valueOf(h.getQuantity());
      BigDecimal marketValue = effective
        .multiply(qty)
        .setScale(2, RoundingMode.HALF_UP);
      BigDecimal cost = h.getAvgCost().multiply(qty);
      BigDecimal profit = marketValue.subtract(cost).setScale(2, RoundingMode.HALF_UP);
      BigDecimal returnRate = cost.signum() == 0
        ? BigDecimal.ZERO
        : profit.divide(cost, 6, RoundingMode.HALF_UP);

      holdingsValue = holdingsValue.add(marketValue);
      positions.add(
        new PositionResponse(
          h.getSymbol(),
          symbolService.findNameOrSymbol(h.getSymbol()),
          h.getQuantity(),
          h.getAvgCost(),
          price,
          marketValue,
          profit,
          returnRate,
          stale
        )
      );
    }

    return new PortfolioResponse(
      summarize(account, holdingsValue, anyStale),
      positions
    );
  }

  /**
   * 다른 사용자가 보는 포트폴리오. 계좌를 새로 열지 않고 읽기만 한다.
   * 금액·수량은 내보내지 않고 총 자산 대비 비중과 종목별 수익률만 준다.
   */
  @Transactional(readOnly = true)
  public PublicPortfolioResponse getPublicPortfolio(UUID targetUserId) {
    AuthenticatedUser target = memberLookup
      .findById(targetUserId)
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")
      );
    InvestAccount account = accountRepository
      .findByUserId(targetUserId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "모의투자에 참여하지 않은 사용자입니다."
        )
      );

    List<Holding> holdings = holdingRepository.findByAccountIdOrderBySymbolAsc(
      account.getId()
    );

    record Valued(Holding holding, BigDecimal value) {}
    List<Valued> valued = new ArrayList<>();
    BigDecimal holdingsValue = BigDecimal.ZERO;
    boolean anyStale = false;
    for (Holding h : holdings) {
      Optional<PriceService.QuoteView> view = priceService.findQuoteForValuation(h.getSymbol());
      BigDecimal price = view.map(v -> v.quote().price()).orElse(h.getAvgCost());
      anyStale |= view.map(PriceService.QuoteView::stale).orElse(true);
      BigDecimal value = price.multiply(BigDecimal.valueOf(h.getQuantity()));
      holdingsValue = holdingsValue.add(value);
      valued.add(new Valued(h, value));
    }

    BigDecimal equity = account.getCash().add(holdingsValue);
    List<PublicPositionResponse> positions = new ArrayList<>();
    for (Valued v : valued) {
      BigDecimal cost = v.holding().getAvgCost().multiply(
        BigDecimal.valueOf(v.holding().getQuantity())
      );
      BigDecimal rate = cost.signum() == 0
        ? BigDecimal.ZERO
        : v.value().subtract(cost).divide(cost, 6, RoundingMode.HALF_UP);
      positions.add(
        new PublicPositionResponse(
          v.holding().getSymbol(),
          symbolService.findNameOrSymbol(v.holding().getSymbol()),
          ratio(v.value(), equity),
          rate
        )
      );
    }
    positions.sort((a, b) -> b.weight().compareTo(a.weight()));

    Optional<RankingEntry> entry = rankingService.findEntry(targetUserId);
    BigDecimal returnRate = equity
      .subtract(account.getSeedMoney())
      .divide(account.getSeedMoney(), 6, RoundingMode.HALF_UP);

    return new PublicPortfolioResponse(
      targetUserId,
      target.nickname(),
      entry.map(RankingEntry::rank).orElse(null),
      returnRate,
      ratio(account.getCash(), equity),
      positions,
      anyStale
    );
  }

  private static BigDecimal ratio(BigDecimal part, BigDecimal total) {
    return total.signum() == 0
      ? BigDecimal.ZERO
      : part.divide(total, 6, RoundingMode.HALF_UP);
  }

  /** 계좌 요약만 필요할 때. 포트폴리오 전체를 계산한 뒤 요약만 떼어 준다. */
  @Transactional
  public AccountSummaryResponse getSummary(UUID userId) {
    return getPortfolio(userId).summary();
  }

  static AccountSummaryResponse summarize(
    InvestAccount account,
    BigDecimal holdingsValue,
    boolean pricesStale
  ) {
    BigDecimal equity = account.getCash().add(holdingsValue);
    BigDecimal profit = equity.subtract(account.getSeedMoney());
    BigDecimal returnRate = profit.divide(
      account.getSeedMoney(),
      6,
      RoundingMode.HALF_UP
    );
    return new AccountSummaryResponse(
      account.getCash(),
      account.getSeedMoney(),
      holdingsValue.setScale(2, RoundingMode.HALF_UP),
      equity.setScale(2, RoundingMode.HALF_UP),
      profit.setScale(2, RoundingMode.HALF_UP),
      returnRate,
      pricesStale
    );
  }
}
