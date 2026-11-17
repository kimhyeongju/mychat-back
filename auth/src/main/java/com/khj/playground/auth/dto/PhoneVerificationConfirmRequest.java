package com.khj.playground.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationConfirmRequest(
  @NotBlank(message = "{user-dto.phone-number.not-blank}")
  @Pattern(regexp = "^01[0-9]{8,9}$", message = "{user-dto.phone-number.pattern}")
  String phoneNumber,

  @NotBlank(message = "{user-dto.verification-code.not-blank}")
  @Pattern(regexp = "^[0-9]{6}$", message = "{user-dto.verification-code.pattern}")
  String code
) {}
