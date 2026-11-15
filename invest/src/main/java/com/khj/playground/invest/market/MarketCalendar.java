package com.khj.playground.invest.market;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 미국 증시(NYSE) 휴장일과 조기 폐장 일정, 그리고 "주간 정산 시점"을 계산한다.
 * 외부 API 없이 규칙만으로 계산하므로 시세 제공자와 무관하게 동작한다.
 *
 * - 휴장: 신년, MLK 데이, 대통령의 날, 성금요일, 메모리얼 데이, 준틴스, 독립기념일, 노동절, 추수감사절, 성탄절
 *   (토요일이면 금요일, 일요일이면 월요일에 쉰다. 단 신년이 토요일이면 쉬지 않는다.)
 * - 13:00 조기 폐장: 추수감사절 다음 날, 12/24, 7/3 (평일이고 휴장이 아닐 때)
 *
 * 한 주는 월~금이며, 그 주의 마지막 거래일 폐장 시각이 주 마감이다.
 * 정산은 폐장 직후 시세가 안정되도록 SETTLE_DELAY 뒤에 한다.
 */
public final class MarketCalendar {

  public static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
  public static final Duration SETTLE_DELAY = Duration.ofMinutes(2);

  private static final LocalTime CLOSE = LocalTime.of(16, 0);
  private static final LocalTime EARLY_CLOSE = LocalTime.of(13, 0);

  private MarketCalendar() {}

  public static boolean isTradingDay(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
      return false;
    }
    return !isHoliday(date);
  }

  public static LocalTime closeTime(LocalDate date) {
    return isEarlyClose(date) ? EARLY_CLOSE : CLOSE;
  }

  /** 그 주(월~금)의 마지막 거래일. 거래일이 하나도 없는 주는 없다고 본다. */
  public static LocalDate lastTradingDayOfWeek(LocalDate monday) {
    for (int i = 4; i >= 0; i--) {
      LocalDate d = monday.plusDays(i);
      if (isTradingDay(d)) return d;
    }
    return monday.plusDays(4);
  }

  /** 주 마감일(week_end_date)의 정산 시각. */
  public static Instant settlementAt(LocalDate weekEnd) {
    return ZonedDateTime
      .of(weekEnd, closeTime(weekEnd), NEW_YORK)
      .toInstant()
      .plus(SETTLE_DELAY);
  }

  /** 지금 진행 중인 주의 마감일 = 정산 시각이 now 보다 뒤인 가장 이른 주. */
  public static LocalDate currentWeekEnd(Instant now) {
    LocalDate today = now.atZone(NEW_YORK).toLocalDate();
    LocalDate monday = isWeekday(today)
      ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
      : today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    LocalDate end = lastTradingDayOfWeek(monday);
    if (!settlementAt(end).isAfter(now)) {
      end = lastTradingDayOfWeek(monday.plusWeeks(1));
    }
    return end;
  }

  /** 정산 시각이 이미 지난 주들 중 가장 최근 주의 마감일. */
  public static LocalDate latestDueWeekEnd(Instant now) {
    return previousWeekEnd(currentWeekEnd(now));
  }

  public static LocalDate previousWeekEnd(LocalDate weekEnd) {
    LocalDate monday = weekEnd.with(
      TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    );
    return lastTradingDayOfWeek(monday.minusWeeks(1));
  }

  private static boolean isWeekday(LocalDate d) {
    DayOfWeek day = d.getDayOfWeek();
    return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
  }

  private static boolean isEarlyClose(LocalDate d) {
    if (!isTradingDay(d)) return false;
    int year = d.getYear();
    if (d.equals(thanksgiving(year).plusDays(1))) return true;
    if (d.getMonth() == Month.DECEMBER && d.getDayOfMonth() == 24) return true;
    return d.getMonth() == Month.JULY && d.getDayOfMonth() == 3;
  }

  private static boolean isHoliday(LocalDate d) {
    int y = d.getYear();
    return (
      d.equals(newYear(y)) ||
      d.equals(nthWeekday(y, Month.JANUARY, DayOfWeek.MONDAY, 3)) ||
      d.equals(nthWeekday(y, Month.FEBRUARY, DayOfWeek.MONDAY, 3)) ||
      d.equals(easter(y).minusDays(2)) ||
      d.equals(
        LocalDate.of(y, Month.MAY, 31).with(
          TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY)
        )
      ) ||
      d.equals(observed(LocalDate.of(y, Month.JUNE, 19))) ||
      d.equals(observed(LocalDate.of(y, Month.JULY, 4))) ||
      d.equals(nthWeekday(y, Month.SEPTEMBER, DayOfWeek.MONDAY, 1)) ||
      d.equals(thanksgiving(y)) ||
      d.equals(observed(LocalDate.of(y, Month.DECEMBER, 25)))
    );
  }

  /** 신년은 토요일이면 쉬지 않고, 일요일이면 월요일에 쉰다. */
  private static LocalDate newYear(int year) {
    LocalDate d = LocalDate.of(year, Month.JANUARY, 1);
    if (d.getDayOfWeek() == DayOfWeek.SATURDAY) return LocalDate.of(1900, 1, 1);
    return observed(d);
  }

  private static LocalDate observed(LocalDate d) {
    return switch (d.getDayOfWeek()) {
      case SATURDAY -> d.minusDays(1);
      case SUNDAY -> d.plusDays(1);
      default -> d;
    };
  }

  private static LocalDate thanksgiving(int year) {
    return nthWeekday(year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4);
  }

  private static LocalDate nthWeekday(
    int year,
    Month month,
    DayOfWeek day,
    int n
  ) {
    return LocalDate
      .of(year, month, 1)
      .with(TemporalAdjusters.dayOfWeekInMonth(n, day));
  }

  /** 부활절(그레고리력). 성금요일 계산에 쓴다. */
  static LocalDate easter(int year) {
    int a = year % 19;
    int b = year / 100;
    int c = year % 100;
    int d = b / 4;
    int e = b % 4;
    int f = (b + 8) / 25;
    int g = (b - f + 1) / 3;
    int h = (19 * a + b - d - g + 15) % 30;
    int i = c / 4;
    int k = c % 4;
    int l = (32 + 2 * e + 2 * i - h - k) % 7;
    int m = (a + 11 * h + 22 * l) / 451;
    int month = (h + l - 7 * m + 114) / 31;
    int day = ((h + l - 7 * m + 114) % 31) + 1;
    return LocalDate.of(year, month, day);
  }
}
