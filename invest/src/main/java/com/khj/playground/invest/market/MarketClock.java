package com.khj.playground.invest.market;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * 미국 정규장 개장 여부. 캐시 유효기간을 정하는 데만 쓴다.
 * 공휴일은 고려하지 않는다. 휴일에는 가격이 안 움직이므로 조금 더 자주 조회할 뿐 틀리지는 않는다.
 */
@Component
public class MarketClock {

  private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
  private static final LocalTime OPEN = LocalTime.of(9, 30);
  private static final LocalTime CLOSE = LocalTime.of(16, 0);

  public boolean isRegularSessionOpen() {
    ZonedDateTime now = ZonedDateTime.now(NEW_YORK);
    DayOfWeek day = now.getDayOfWeek();
    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
      return false;
    }
    LocalTime time = now.toLocalTime();
    return !time.isBefore(OPEN) && time.isBefore(CLOSE);
  }
}
