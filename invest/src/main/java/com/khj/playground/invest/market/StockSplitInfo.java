package com.khj.playground.invest.market;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 분할/병합 한 건. old 주가 new 주가 된다.
 * 분할이면 newRate > oldRate, 병합이면 newRate < oldRate 로 정규화해서 넘긴다.
 */
public record StockSplitInfo(
  String symbol,
  LocalDate exDate,
  BigDecimal newRate,
  BigDecimal oldRate
) {}
