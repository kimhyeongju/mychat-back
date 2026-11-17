package com.khj.playground.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
  @NotBlank(message = "{user-dto.username.not-blank}")
  @Size(min = 4, max = 20, message = "{user-dto.username.size}")
  @Pattern(
    regexp = "^[a-zA-Z0-9_]+$",
    message = "{user-dto.username.pattern}"
  )
  String username,

  @NotBlank(message = "{user-dto.password.not-blank}")
  @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
  String password,

  @NotBlank(message = "{user-dto.nickname.not-blank}")
  @Size(min = 2, max = 12, message = "{user-dto.nickname.size}")
  String nickname,

  @NotBlank(message = "{user-dto.phone-number.not-blank}")
  @Pattern(regexp = "^01[0-9]{8,9}$", message = "{user-dto.phone-number.pattern}")
  String phoneNumber,

  // 필수 아님
  @Email(message = "{user-dto.email.email}") String email
) {}
