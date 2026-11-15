package com.khj.playground.invest.service;

import com.khj.playground.invest.market.MarketCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * "지금은 어느 주인가"의 단일 창구. 거래·랭킹·정산이 모두 이걸로 같은 주를 가리킨다.
 * 주 식별자는 그 주의 마감일(week_end_date)이다.
 * 정산 중에는 주문을 잠시 막기 위한 플래그도 여기 둔다(단일 인스턴스 전제).
 */
@Component
public class WeeklyPeriod {

  private final AtomicBoolean settling = new AtomicBoolean(false);

  public LocalDate currentWeekEnd() {
    return MarketCalendar.currentWeekEnd(Instant.now());
  }

  public Instant currentSettlementAt() {
    return MarketCalendar.settlementAt(currentWeekEnd());
  }

  public boolean isSettling() {
    return settling.get();
  }

  /** @return 이미 정산 중이면 false */
  boolean beginSettling() {
    return settling.compareAndSet(false, true);
  }

  void endSettling() {
    settling.set(false);
  }
}
