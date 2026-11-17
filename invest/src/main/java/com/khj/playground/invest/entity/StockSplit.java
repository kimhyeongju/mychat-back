package com.khj.playground.invest.entity;

import com.khj.playground.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** 종목의 분할/병합 일정. 적용(applied_at)되기 전까지는 대기 상태다. */
@Getter
@Entity
@Table(name = "invest_stock_splits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockSplit extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @Column(nullable = false, length = 20)
  private String symbol;

  @Column(name = "ex_date", nullable = false)
  private LocalDate exDate;

  @Column(name = "new_rate", nullable = false, precision = 18, scale = 6)
  private BigDecimal newRate;

  @Column(name = "old_rate", nullable = false, precision = 18, scale = 6)
  private BigDecimal oldRate;

  @Column(name = "late_discovery", nullable = false)
  private boolean lateDiscovery;

  @Column(name = "applied_at")
  private LocalDateTime appliedAt;

  public static StockSplit of(
    String symbol,
    LocalDate exDate,
    BigDecimal newRate,
    BigDecimal oldRate,
    boolean lateDiscovery
  ) {
    StockSplit split = new StockSplit();
    split.symbol = symbol;
    split.exDate = exDate;
    split.newRate = newRate;
    split.oldRate = oldRate;
    split.lateDiscovery = lateDiscovery;
    return split;
  }

  public boolean isApplied() {
    return appliedAt != null;
  }

  public void markApplied() {
    this.appliedAt = LocalDateTime.now();
  }
}
