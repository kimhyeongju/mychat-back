package com.khj.playground.invest.dto;

import java.math.BigDecimal;

/** 보유 종목 한 줄. price 가 null 이면 시세를 구하지 못한 것이고 평가는 평균단가로 대신한다. */
public record PositionResponse(
  String symbol,
  String name,
  long quantity,
  BigDecimal avgCost,
  BigDecimal price,
  BigDecimal marketValue,
  BigDecimal profit,
  BigDecimal returnRate,
  boolean stale
) {}
