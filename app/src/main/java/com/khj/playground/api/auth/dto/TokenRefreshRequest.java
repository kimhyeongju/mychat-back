package com.khj.playground.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(@NotBlank String refreshToken) {}
