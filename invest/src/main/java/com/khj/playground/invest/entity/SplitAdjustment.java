package com.khj.playground.invest.entity;

import com.khj.playground.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** 계좌 하나에 분할/병합을 적용한 기록. 거래 내역과 별개로 남긴다. */
@Getter
@Entity
@Table(name = "invest_split_adjustments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SplitAdjustment extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @Column(name = "split_id", nullable = false)
  private UUID splitId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private InvestAccount account;

  @Column(nullable = false, length = 20)
  private String symbol;

  @Column(name = "new_rate", nullable = false, precision = 18, scale = 6)
  private BigDecimal newRate;

  @Column(name = "old_rate", nullable = false, precision = 18, scale = 6)
  private BigDecimal oldRate;

  @Column(name = "qty_before", nullable = false)
  private long qtyBefore;

  @Column(name = "qty_after", nullable = false)
  private long qtyAfter;

  @Column(name = "avg_cost_before", nullable = false, precision = 18, scale = 4)
  private BigDecimal avgCostBefore;

  @Column(name = "avg_cost_after", nullable = false, precision = 18, scale = 4)
  private BigDecimal avgCostAfter;

  @Column(name = "cash_in_lieu", nullable = false, precision = 18, scale = 2)
  private BigDecimal cashInLieu;

  @Column(nullable = false)
  private boolean skipped;

  @Column(name = "week_end_date")
  private LocalDate weekEndDate;

  public static SplitAdjustment of(
    StockSplit split,
    InvestAccount account,
    long qtyBefore,
    long qtyAfter,
    BigDecimal avgCostBefore,
    BigDecimal avgCostAfter,
    BigDecimal cashInLieu,
    boolean skipped,
    LocalDate weekEndDate
  ) {
    SplitAdjustment a = new SplitAdjustment();
    a.splitId = split.getId();
    a.account = account;
    a.symbol = split.getSymbol();
    a.newRate = split.getNewRate();
    a.oldRate = split.getOldRate();
    a.qtyBefore = qtyBefore;
    a.qtyAfter = qtyAfter;
    a.avgCostBefore = avgCostBefore;
    a.avgCostAfter = avgCostAfter;
    a.cashInLieu = cashInLieu;
    a.skipped = skipped;
    a.weekEndDate = weekEndDate;
    return a;
  }
}
