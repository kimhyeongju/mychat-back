package com.khj.playground.invest.entity;

import com.khj.playground.common.entity.BaseTimeEntity;
import com.khj.playground.invest.enums.TradeSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** 체결 내역. 수정하지 않는다. */
@Getter
@Entity
@Table(name = "invest_trades")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private InvestAccount account;

  @Column(nullable = false, length = 20)
  private String symbol;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TradeSide side;

  @Column(nullable = false)
  private long quantity;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal price;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  /** 이 거래가 속한 주의 마감일. 주간 정산에서 그 주에 거래한 사용자를 가린다. */
  @Column(name = "week_end_date")
  private LocalDate weekEndDate;

  public static Trade of(
    InvestAccount account,
    String symbol,
    TradeSide side,
    long quantity,
    BigDecimal price,
    LocalDate weekEndDate
  ) {
    Trade trade = new Trade();
    trade.account = account;
    trade.symbol = symbol;
    trade.side = side;
    trade.quantity = quantity;
    trade.price = price;
    trade.weekEndDate = weekEndDate;
    trade.amount = price
      .multiply(BigDecimal.valueOf(quantity))
      .setScale(2, RoundingMode.HALF_UP);
    return trade;
  }
}
