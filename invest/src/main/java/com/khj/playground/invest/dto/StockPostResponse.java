package com.khj.playground.invest.dto;

import com.khj.playground.invest.entity.StockPost;
import java.time.LocalDateTime;
import java.util.UUID;

/** liked/mine 은 요청한 사용자 기준이다. 비로그인이면 둘 다 false. */
public record StockPostResponse(
  Long id,
  String symbol,
  String content,
  UUID authorId,
  String authorNickname,
  int likeCount,
  int commentCount,
  boolean liked,
  boolean mine,
  LocalDateTime createdAt,
  boolean edited
) {
  public static StockPostResponse of(
    StockPost post,
    String authorNickname,
    boolean liked,
    boolean mine
  ) {
    return new StockPostResponse(
      post.getId(),
      post.getSymbol(),
      post.getContent(),
      post.getUserId(),
      authorNickname,
      post.getLikeCount(),
      post.getCommentCount(),
      liked,
      mine,
      post.getCreatedAt(),
      post.getUpdatedAt() != null
    );
  }
}
