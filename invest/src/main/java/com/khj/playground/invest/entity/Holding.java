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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** 계좌·종목별 보유 수량과 평균 매수 단가. 수량이 0이 되면 행을 지운다. */
@Getter
@Entity
@Table(
  name = "invest_holdings",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_invest_holdings_account_symbol",
      columnNames = { "account_id", "symbol" }
    ),
  }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private InvestAccount account;

  @Column(nullable = false, length = 20)
  private String symbol;

  @Column(nullable = false)
  private long quantity;

  @Column(name = "avg_cost", nullable = false, precision = 18, scale = 4)
  private BigDecimal avgCost;

  public static Holding open(InvestAccount account, String symbol) {
    Holding holding = new Holding();
    holding.account = account;
    holding.symbol = symbol;
    holding.quantity = 0;
    holding.avgCost = BigDecimal.ZERO;
    return holding;
  }

  /** 추가 매수. 평균 단가를 가중 평균으로 다시 계산한다. */
  public void buy(long qty, BigDecimal price) {
    BigDecimal currentValue = avgCost.multiply(BigDecimal.valueOf(quantity));
    BigDecimal addedValue = price.multiply(BigDecimal.valueOf(qty));
    long newQuantity = quantity + qty;
    this.avgCost =
      currentValue
        .add(addedValue)
        .divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);
    this.quantity = newQuantity;
  }

  /** 매도. 평균 단가는 유지한다. */
  public void sell(long qty) {
    if (qty > quantity) {
      throw new IllegalStateException("보유 수량 부족");
    }
    this.quantity -= qty;
  }

  /** 분할/병합 조정. 수량과 평균 단가를 함께 바꿔 매수 원가를 유지한다. */
  public void applySplit(long newQuantity, BigDecimal newAvgCost) {
    this.quantity = newQuantity;
    this.avgCost = newAvgCost;
  }

  public boolean isEmpty() {
    return quantity == 0;
  }
}
