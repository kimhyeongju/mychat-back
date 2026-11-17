package com.khj.playground.invest.service;

import com.khj.playground.invest.entity.InvestWeek;
import com.khj.playground.invest.market.MarketCalendar;
import com.khj.playground.invest.repository.HoldingRepository;
import com.khj.playground.invest.repository.InvestWeekRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 30초마다 "정산할 주가 있는가"를 확인한다. 정산 시각(주 마감 + 2분)이 지났고 아직 기록이 없으면 정산한다.
 * 서버가 마감 때 꺼져 있었어도 다시 켜지면 가장 최근에 마감된 주 하나를 따라잡는다.
 *
 * 처음 배포했을 때 이미 지나간 주를 소급 정산하지 않도록, 기록이 하나도 없을 때는
 * invest.weekly.first-week-end(비어 있으면 기동 시점의 이번 주 마감일) 이전의 주는 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklySettlementScheduler {

  private final WeeklySettlementService settlementService;
  private final InvestWeekRepository weekRepository;
  private final HoldingRepository holdingRepository;
  private final PriceService priceService;
  private final RankingService rankingService;
  private final ScoreService scoreService;
  private final WeeklyPeriod weeklyPeriod;
  private final StockSplitService splitService;

  @Value("${invest.weekly.enabled:true}")
  private boolean enabled;

  @Value("${invest.weekly.first-week-end:}")
  private String firstWeekEnd;

  private final LocalDate bootWeekEnd = MarketCalendar.currentWeekEnd(
    Instant.now()
  );

  @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
  public void run() {
    if (!enabled) return;

    LocalDate due = MarketCalendar.latestDueWeekEnd(Instant.now());
    if (weekRepository.existsByWeekEndDate(due) || !shouldSettle(due)) {
      return;
    }
    if (!weeklyPeriod.beginSettling()) {
      return;
    }
    try {
      refreshPrices();
      applyPendingSplits();
      if (settlementService.settle(due)) {
        rankingService.invalidate();
        scoreService.invalidate();
      }
    } catch (Exception e) {
      log.error("주간 정산 실패 weekEnd={}", due, e);
    } finally {
      weeklyPeriod.endSettling();
    }
  }

  private boolean shouldSettle(LocalDate due) {
    Optional<InvestWeek> last = weekRepository.findFirstByOrderByWeekEndDateDesc();
    if (last.isPresent()) {
      return due.isAfter(last.get().getWeekEndDate());
    }
    LocalDate first = firstWeekEnd == null || firstWeekEnd.isBlank()
      ? bootWeekEnd
      : LocalDate.parse(firstWeekEnd.trim());
    return !due.isBefore(first);
  }

  /** 마감 직전에 아직 반영 안 된 분할/병합이 있으면 먼저 반영한다. 실패해도 정산은 진행한다. */
  private void applyPendingSplits() {
    try {
      splitService.applyDue();
    } catch (Exception e) {
      log.warn("정산 직전 분할/병합 반영 실패: {}", e.getMessage());
    }
  }

  /** 마감 시점 가격이 최신이 되도록 보유 종목을 한 번 갱신한다. 실패해도 정산은 진행한다. */
  private void refreshPrices() {
    try {
      List<String> symbols = holdingRepository.findDistinctSymbols();
      if (priceService.supportsBatch()) {
        priceService.refreshBatch(symbols);
      } else {
        symbols.forEach(priceService::refreshIfStale);
      }
    } catch (Exception e) {
      log.warn("정산 직전 시세 갱신 실패, 캐시된 시세로 진행합니다: {}", e.getMessage());
    }
  }
}
