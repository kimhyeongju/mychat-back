package com.khj.playground.invest.entity;

import com.khj.playground.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 종목 이름 캐시. 시세 API 는 심볼만 주고 이름은 별도 호출이 필요해서,
 * 한 번 알아낸 이름은 DB 에 남겨 호출을 아낀다.
 */
@Getter
@Entity
@Table(name = "invest_symbols")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockSymbol extends BaseTimeEntity {

  @Id
  @Column(length = 20)
  private String symbol;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 50)
  private String exchange;

  public static StockSymbol of(String symbol, String name, String exchange) {
    StockSymbol entity = new StockSymbol();
    entity.symbol = symbol;
    entity.name = name;
    entity.exchange = exchange;
    return entity;
  }
}
