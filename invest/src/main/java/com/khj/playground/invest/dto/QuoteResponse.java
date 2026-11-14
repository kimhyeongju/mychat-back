package com.khj.playground.invest.dto;

import com.khj.playground.invest.market.Quote;
import java.math.BigDecimal;
import java.time.Instant;

/** @param stale 최신 조회에 실패해 이전 시세를 보여주는 중이면 true */
public record QuoteResponse(
  String symbol,
  String name,
  BigDecimal price,
  BigDecimal change,
  BigDecimal changePercent,
  BigDecimal previousClose,
  BigDecimal high,
  BigDecimal low,
  BigDecimal open,
  Instant updatedAt,
  boolean stale,
  boolean marketOpen
) {
  public static QuoteResponse of(
    Quote q,
    String name,
    boolean stale,
    boolean marketOpen
  ) {
    return new QuoteResponse(
      q.symbol(),
      name,
      q.price(),
      q.change(),
      q.changePercent(),
      q.previousClose(),
      q.high(),
      q.low(),
      q.open(),
      q.fetchedAt(),
      stale,
      marketOpen
    );
  }
}
