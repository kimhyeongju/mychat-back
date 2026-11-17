package com.khj.playground.invest.dto;

import com.khj.playground.invest.entity.StockComment;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockCommentResponse(
  Long id,
  Long postId,
  String content,
  UUID authorId,
  String authorNickname,
  boolean mine,
  LocalDateTime createdAt
) {
  public static StockCommentResponse of(
    StockComment comment,
    String authorNickname,
    boolean mine
  ) {
    return new StockCommentResponse(
      comment.getId(),
      comment.getPostId(),
      comment.getContent(),
      comment.getUserId(),
      authorNickname,
      mine,
      comment.getCreatedAt()
    );
  }
}
