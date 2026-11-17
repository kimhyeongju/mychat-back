package com.khj.playground.invest.dto;

import com.khj.playground.invest.entity.SplitAdjustment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 이번 주에 내 계좌에 적용된 분할/병합 조정 한 건. */
public record SplitAdjustmentResponse(
  String symbol,
  /** 분할이면 newRate > oldRate (4:1 → 4, 1), 병합이면 newRate < oldRate (1:10 → 1, 10) */
  BigDecimal newRate,
  BigDecimal oldRate,
  long qtyBefore,
  long qtyAfter,
  BigDecimal avgCostBefore,
  BigDecimal avgCostAfter,
  /** 병합으로 생긴 소수 주를 현금으로 정산한 금액 */
  BigDecimal cashInLieu,
  /** true 이면 조정하지 못한 건이다(분할 후 거래와 섞여 자동 조정 불가) */
  boolean skipped,
  LocalDateTime appliedAt
) {
  public static SplitAdjustmentResponse from(SplitAdjustment a) {
    return new SplitAdjustmentResponse(
      a.getSymbol(),
      a.getNewRate().stripTrailingZeros(),
      a.getOldRate().stripTrailingZeros(),
      a.getQtyBefore(),
      a.getQtyAfter(),
      a.getAvgCostBefore(),
      a.getAvgCostAfter(),
      a.getCashInLieu(),
      a.isSkipped(),
      a.getCreatedAt()
    );
  }
}
