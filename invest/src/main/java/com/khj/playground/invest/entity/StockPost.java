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

/**
 * 종목 토론 글. like_count/comment_count 는 정렬과 목록 표시를 위해 들고 있는 값이고,
 * 좋아요·댓글을 바꿀 때마다 실제 건수로 다시 계산해 맞춘다.
 */
@Getter
@Entity
@Table(name = "invest_stock_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockPost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String symbol;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 1000)
  private String content;

  @Column(name = "like_count", nullable = false)
  private int likeCount;

  @Column(name = "comment_count", nullable = false)
  private int commentCount;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public static StockPost of(String symbol, UUID userId, String content) {
    StockPost post = new StockPost();
    post.symbol = symbol;
    post.userId = userId;
    post.content = content;
    post.createdAt = LocalDateTime.now();
    return post;
  }

  public void edit(String content) {
    this.content = content;
    this.updatedAt = LocalDateTime.now();
  }
}
