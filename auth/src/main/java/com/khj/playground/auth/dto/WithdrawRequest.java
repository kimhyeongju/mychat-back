package com.khj.playground.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(@NotBlank String password) {}
