package com.khj.playground.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
  @NotBlank @Size(max = 200) String title,
  @NotBlank @Size(max = 20000) String content
) {}
