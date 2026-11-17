package com.khj.playground.invest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StockPostRequest(
  @NotBlank(message = "{invest-dto.post.content.not-blank}")
  @Size(max = 1000, message = "{invest-dto.post.content.size}")
  String content
) {}
