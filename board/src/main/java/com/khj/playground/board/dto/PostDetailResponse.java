package com.khj.playground.board.dto;

import com.khj.playground.board.entity.Post;
import java.time.LocalDateTime;
import java.util.UUID;

public record PostDetailResponse(
  UUID id,
  String boardSlug,
  String title,
  String content,
  UUID authorId,
  String authorNickname,
  long viewCount,
  int commentCount,
  LocalDateTime createdAt,
  LocalDateTime updatedAt,
  /** 프론트가 수정/삭제 버튼을 보일지 판단할 수 있게 서버가 알려준다. */
  boolean editable
) {
  public static PostDetailResponse from(
    Post post,
    boolean editable,
    long viewCount
  ) {
    return new PostDetailResponse(
      post.getId(),
      post.getBoard().getSlug(),
      post.getTitle(),
      post.getContent(),
      post.getAuthorId(),
      post.getAuthorNickname(),
      viewCount,
      post.getCommentCount(),
      post.getCreatedAt(),
      post.getUpdatedAt(),
      editable
    );
  }

  /** 조회수 변동이 없는 경우(생성/수정 응답)에 쓴다. */
  public static PostDetailResponse from(Post post, boolean editable) {
    return from(post, editable, post.getViewCount());
  }
}
