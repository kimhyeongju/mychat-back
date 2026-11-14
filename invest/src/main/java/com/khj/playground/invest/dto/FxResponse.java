package com.khj.playground.invest.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** @param source live(방금 조회) | stale(이전 값) | fallback(고정 추정치) */
public record FxResponse(
  String base,
  String quote,
  BigDecimal rate,
  Instant updatedAt,
  String source
) {}
