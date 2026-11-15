package com.khj.playground.invest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자의 관심 종목 한 건. */
@Getter
@Entity
@Table(name = "invest_watchlist")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 20)
  private String symbol;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public static WatchlistItem of(UUID userId, String symbol) {
    WatchlistItem item = new WatchlistItem();
    item.userId = userId;
    item.symbol = symbol;
    item.createdAt = LocalDateTime.now();
    return item;
  }
}
