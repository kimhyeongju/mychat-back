package com.khj.playground.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
  @NotBlank(message = "{board-dto.comment-content.not-blank}")
  @Size(max = 1000, message = "{board-dto.comment-content.size}")
  String content
) {}
