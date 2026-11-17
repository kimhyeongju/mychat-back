package com.khj.playground.invest.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 분할/병합 계산. 스프링과 무관한 순수 계산이라 단독으로 검증할 수 있다.
 *
 * 비율은 newRate / oldRate 다. (4:1 분할 → 4/1, 1:10 병합 → 1/10)
 * - 수량: 보유 수량 × 비율, 소수점은 버린다. 버린 소수 주는 분할 후 시세로 현금 정산한다.
 * - 평균 단가: 단가 × oldRate / newRate. 매수 원가가 그대로 유지되므로 수익률이 변하지 않는다.
 */
public final class SplitMath {

  private static final int EXACT_SCALE = 10;

  private SplitMath() {}

  /**
   * @param newQuantity 조정 후 정수 수량 (0 일 수 있다)
   * @param newAvgCost 조정 후 평균 단가
   * @param fractionalShares 정수로 만들지 못하고 남은 소수 주 (0 이상 1 미만)
   */
  public record Result(long newQuantity, BigDecimal newAvgCost, BigDecimal fractionalShares) {

    /** 소수 주를 분할 후 시세로 환산한 현금. */
    public BigDecimal cashInLieu(BigDecimal postSplitPrice) {
      return fractionalShares
        .multiply(postSplitPrice)
        .setScale(2, RoundingMode.HALF_UP);
    }
  }

  public static Result adjust(
    long quantity,
    BigDecimal avgCost,
    BigDecimal newRate,
    BigDecimal oldRate
  ) {
    if (quantity < 0 || newRate.signum() <= 0 || oldRate.signum() <= 0) {
      throw new IllegalArgumentException("잘못된 분할 입력");
    }
    BigDecimal exact = BigDecimal
      .valueOf(quantity)
      .multiply(newRate)
      .divide(oldRate, EXACT_SCALE, RoundingMode.HALF_UP);
    BigDecimal whole = exact.setScale(0, RoundingMode.DOWN);
    BigDecimal fraction = exact.subtract(whole);
    BigDecimal newAvg = avgCost
      .multiply(oldRate)
      .divide(newRate, 4, RoundingMode.HALF_UP);
    return new Result(whole.longValueExact(), newAvg, fraction);
  }
}
