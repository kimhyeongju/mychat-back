package com.khj.mychatback.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(@NotBlank String password) {}
