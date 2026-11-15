package com.khj.playground.invest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 한글 종목명 검색용 별칭. 마이그레이션으로만 채우므로 읽기 전용이다. */
@Getter
@Entity
@Table(name = "invest_symbol_aliases")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SymbolAlias {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String symbol;

  @Column(nullable = false, length = 100)
  private String alias;

  @Column(name = "name_en", nullable = false, length = 200)
  private String nameEn;
}
