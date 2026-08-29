package com.khj.mychatback.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
  @NotBlank String username,

  @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phoneNumber,

  @NotBlank @Size(min = 8, max = 64) String newPassword
) {}
