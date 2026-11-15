package com.khj.playground.invest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @param leaderboardRank 누적 포인트 순위. 포인트가 없으면 null
 * @param recent 최근 10주 성적(최신순)
 */
public record ProfileResponse(
  UUID userId,
  String nickname,
  int totalPoints,
  int firstPlaces,
  int secondPlaces,
  int thirdPlaces,
  int weeksPlayed,
  Integer leaderboardRank,
  List<WeekRecord> recent
) {
  public record WeekRecord(
    LocalDate weekEndDate,
    int rank,
    BigDecimal returnRate,
    int points
  ) {}
}
