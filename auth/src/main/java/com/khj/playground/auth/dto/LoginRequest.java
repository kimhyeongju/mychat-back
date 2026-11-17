package com.khj.playground.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
  @NotBlank(message = "{user-dto.username.not-blank}") String username,
  @NotBlank(message = "{user-dto.password.not-blank}") String password
) {}
