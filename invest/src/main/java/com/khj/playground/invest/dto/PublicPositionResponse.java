package com.khj.playground.invest.dto;

import java.math.BigDecimal;

/**
 * 다른 사용자에게 공개되는 보유 종목. 수량·평균단가·평가금액은 숨기고 비중과 수익률만 준다.
 *
 * @param weight 총 자산 대비 비중(0.1234 = 12.34%)
 */
public record PublicPositionResponse(
  String symbol,
  String name,
  BigDecimal weight,
  BigDecimal returnRate
) {}
