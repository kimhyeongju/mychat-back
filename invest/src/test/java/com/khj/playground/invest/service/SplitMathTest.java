package com.khj.playground.invest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SplitMathTest {

  private static BigDecimal d(String v) {
    return new BigDecimal(v);
  }

  @Test
  void forwardSplitKeepsCostBasis() {
    SplitMath.Result r = SplitMath.adjust(10, d("100.0000"), d("4"), d("1"));
    assertEquals(40, r.newQuantity());
    assertEquals(0, d("25.0000").compareTo(r.newAvgCost()));
    assertEquals(0, BigDecimal.ZERO.compareTo(r.fractionalShares()));
  }

  @Test
  void oddForwardSplitPaysCashForFraction() {
    // 7주 x 3:2 = 10.5주 → 10주 + 0.5주는 현금(분할 후 $40 → $20)
    SplitMath.Result r = SplitMath.adjust(7, d("50.0000"), d("3"), d("2"));
    assertEquals(10, r.newQuantity());
    assertEquals(0, d("0.5").compareTo(r.fractionalShares()));
    assertEquals(0, d("20.00").compareTo(r.cashInLieu(d("40"))));
  }

  @Test
  void reverseSplitKeepsTotalValue() {
    SplitMath.Result r = SplitMath.adjust(25, d("8.0000"), d("1"), d("10"));
    assertEquals(2, r.newQuantity());
    assertEquals(0, d("80.0000").compareTo(r.newAvgCost()));
    assertEquals(0, d("40.00").compareTo(r.cashInLieu(d("80"))));
  }

  @Test
  void reverseSplitBelowOneShareBecomesCash() {
    SplitMath.Result r = SplitMath.adjust(3, d("8.0000"), d("1"), d("10"));
    assertEquals(0, r.newQuantity());
    assertEquals(0, d("24.00").compareTo(r.cashInLieu(d("80"))));
  }

  @Test
  void rejectsInvalidRates() {
    assertThrows(
      IllegalArgumentException.class,
      () -> SplitMath.adjust(1, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE)
    );
  }
}
