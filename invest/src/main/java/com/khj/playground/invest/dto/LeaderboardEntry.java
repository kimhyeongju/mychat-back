package com.khj.playground.invest.dto;

import java.util.UUID;

/** 누적 포인트 리더보드 한 줄. 순위는 포인트, 1위 횟수, 2위 횟수, 3위 횟수 순으로 가른다. */
public record LeaderboardEntry(
  int rank,
  UUID userId,
  String nickname,
  int totalPoints,
  int firstPlaces,
  int secondPlaces,
  int thirdPlaces,
  int weeksPlayed
) {}
