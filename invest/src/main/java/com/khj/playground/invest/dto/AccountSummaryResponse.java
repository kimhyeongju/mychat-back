package com.khj.playground.invest.dto;

import java.math.BigDecimal;

/**
 * 계좌 요약. 모든 금액은 USD.
 * @param returnRate (equity - seedMoney) / seedMoney, 퍼센트가 아닌 비율(0.05 = 5%)
 * @param pricesStale 보유 종목 중 하나라도 최신 시세가 아니면 true
 */
public record AccountSummaryResponse(
  BigDecimal cash,
  BigDecimal seedMoney,
  BigDecimal holdingsValue,
  BigDecimal equity,
  BigDecimal profit,
  BigDecimal returnRate,
  boolean pricesStale
) {}
