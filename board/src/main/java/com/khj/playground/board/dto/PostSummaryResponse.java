package com.khj.playground.board.dto;

import com.khj.playground.board.entity.Post;
import java.time.LocalDateTime;
import java.util.UUID;

public record PostSummaryResponse(
  UUID id,
  String title,
  String authorNickname,
  long viewCount,
  int commentCount,
  LocalDateTime createdAt
) {
  public static PostSummaryResponse from(Post post) {
    return new PostSummaryResponse(
      post.getId(),
      post.getTitle(),
      post.getAuthorNickname(),
      post.getViewCount(),
      post.getCommentCount(),
      post.getCreatedAt()
    );
  }
}
