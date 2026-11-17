package com.khj.playground.invest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StockCommentRequest(
  @NotBlank(message = "{invest-dto.comment.content.not-blank}")
  @Size(max = 500, message = "{invest-dto.comment.content.size}")
  String content
) {}
