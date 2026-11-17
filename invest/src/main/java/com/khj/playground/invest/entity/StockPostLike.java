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

/** 글 좋아요. (post_id, user_id) 는 유일하다. */
@Getter
@Entity
@Table(name = "invest_stock_post_likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockPostLike {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "post_id", nullable = false)
  private Long postId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public static StockPostLike of(Long postId, UUID userId) {
    StockPostLike like = new StockPostLike();
    like.postId = postId;
    like.userId = userId;
    like.createdAt = LocalDateTime.now();
    return like;
  }
}
