package com.khj.playground.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
  @NotBlank(message = "{user-dto.password.not-blank}") String password
) {}
