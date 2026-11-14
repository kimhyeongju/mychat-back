package com.khj.playground.invest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RankingEntry(
  int rank,
  UUID userId,
  String nickname,
  BigDecimal equity,
  BigDecimal returnRate
) {}
