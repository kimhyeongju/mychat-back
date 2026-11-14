package com.khj.playground.invest.entity;

import com.khj.playground.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * 회원당 하나의 모의투자 계좌. 모든 금액은 USD 다.
 * 원화 표시는 화면에서 환산만 하고 장부에는 섞지 않는다.
 */
@Getter
@Entity
@Table(
  name = "invest_accounts",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_invest_accounts_user", columnNames = "user_id"),
  }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestAccount extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal cash;

  @Column(name = "seed_money", nullable = false, precision = 18, scale = 2)
  private BigDecimal seedMoney;

  public static InvestAccount open(UUID userId, BigDecimal seedMoney) {
    InvestAccount account = new InvestAccount();
    account.userId = userId;
    account.seedMoney = seedMoney;
    account.cash = seedMoney;
    return account;
  }

  public void withdrawCash(BigDecimal amount) {
    if (cash.compareTo(amount) < 0) {
      throw new IllegalStateException("잔고 부족");
    }
    this.cash = cash.subtract(amount);
  }

  public void depositCash(BigDecimal amount) {
    this.cash = cash.add(amount);
  }
}
