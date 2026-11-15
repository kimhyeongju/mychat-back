package com.khj.playground.invest.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 관심 종목 한 줄. 시세를 구하지 못하면 price 등이 null 이다.
 *
 * @param changePercent 퍼센트 단위(1.23 = +1.23%)
 */
public record WatchlistItemResponse(
  String symbol,
  String name,
  BigDecimal price,
  BigDecimal change,
  BigDecimal changePercent,
  Instant tradedAt,
  boolean stale
) {}
