package com.khj.playground.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
  @NotBlank(message = "{user-dto.username.not-blank}") String username,

  @NotBlank(message = "{user-dto.phone-number.not-blank}")
  @Pattern(regexp = "^01[0-9]{8,9}$", message = "{user-dto.phone-number.pattern}")
  String phoneNumber,

  @NotBlank(message = "{user-dto.password.not-blank}")
  @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
  String newPassword
) {}
