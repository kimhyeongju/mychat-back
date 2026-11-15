package com.khj.playground.invest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 이번 주 일정과 규칙, 지난주 결과.
 *
 * @param weekEndDate 이번 주 마감일(ET 날짜)
 * @param settlesAt 정산 시각
 * @param points 1위, 2위, 3위가 받는 포인트
 * @param lastWeek 지난주 결과. 아직 정산된 주가 없으면 null
 */
public record WeekInfoResponse(
  LocalDate weekEndDate,
  Instant settlesAt,
  BigDecimal seedMoney,
  List<Integer> points,
  LastWeek lastWeek
) {
  public record LastWeek(
    LocalDate weekEndDate,
    int participantCount,
    List<Winner> winners
  ) {}

  public record Winner(
    int rank,
    UUID userId,
    String nickname,
    BigDecimal returnRate,
    int points
  ) {}
}
