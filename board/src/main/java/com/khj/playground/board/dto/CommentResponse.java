package com.khj.playground.board.dto;

import com.khj.playground.board.entity.Comment;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
  UUID id,
  String content,
  UUID authorId,
  String authorNickname,
  LocalDateTime createdAt,
  LocalDateTime updatedAt,
  boolean editable
) {
  public static CommentResponse from(Comment comment, boolean editable) {
    return new CommentResponse(
      comment.getId(),
      comment.getContent(),
      comment.getAuthorId(),
      comment.getAuthorNickname(),
      comment.getCreatedAt(),
      comment.getUpdatedAt(),
      editable
    );
  }
}
