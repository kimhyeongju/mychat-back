package com.khj.playground.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
  @NotBlank String username,

  @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phoneNumber,

  @NotBlank
  @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
  String newPassword
) {}
