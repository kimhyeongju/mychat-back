package com.khj.playground.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
  @NotBlank(message = "{user-dto.refresh-token.not-blank}") String refreshToken
) {}
