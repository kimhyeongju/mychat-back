package com.khj.playground.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FindIdRequest(
  @NotBlank(message = "{user-dto.phone-number.not-blank}")
  @Pattern(regexp = "^01[0-9]{8,9}$", message = "{user-dto.phone-number.pattern}")
  String phoneNumber
) {}
