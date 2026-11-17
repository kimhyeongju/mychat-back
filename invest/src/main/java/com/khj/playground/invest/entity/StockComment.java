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

/** 종목 토론 글의 댓글. */
@Getter
@Entity
@Table(name = "invest_stock_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockComment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "post_id", nullable = false)
  private Long postId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false, length = 500)
  private String content;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public static StockComment of(Long postId, UUID userId, String content) {
    StockComment comment = new StockComment();
    comment.postId = postId;
    comment.userId = userId;
    comment.content = content;
    comment.createdAt = LocalDateTime.now();
    return comment;
  }
}
