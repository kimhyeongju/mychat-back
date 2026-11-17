package com.khj.playground.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
  @NotBlank(message = "{board-dto.post-title.not-blank}")
  @Size(max = 200, message = "{board-dto.post-title.size}")
  String title,

  @NotBlank(message = "{board-dto.post-content.not-blank}")
  @Size(max = 20000, message = "{board-dto.post-content.size}")
  String content
) {}
