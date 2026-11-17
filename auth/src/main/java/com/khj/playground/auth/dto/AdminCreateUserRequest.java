package com.khj.playground.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 운영자가 직접 만드는 계정(주로 테스트용). 비밀번호 규칙(PasswordPolicy)은 적용하지 않는다.
 * BCrypt 는 72바이트까지만 보므로 길이만 제한한다.
 * phoneNumber 를 비우면 서버가 겹치지 않는 임시 번호를 만든다.
 */
public record AdminCreateUserRequest(
  @NotBlank(message = "{user-dto.username.not-blank}")
  @Size(min = 4, max = 20, message = "{user-dto.username.size}")
  @Pattern(
    regexp = "^[a-zA-Z0-9_]+$",
    message = "{user-dto.username.pattern}"
  )
  String username,

  @NotBlank(message = "{user-dto.password.not-blank}")
  @Size(min = 1, max = 72, message = "{user-dto.password.size}")
  String password,

  @NotBlank(message = "{user-dto.nickname.not-blank}")
  @Size(min = 2, max = 12, message = "{user-dto.nickname.size}")
  String nickname,

  @Pattern(regexp = "^[0-9]{8,20}$", message = "{user-dto.phone-number.admin-pattern}")
  String phoneNumber,

  @Email(message = "{user-dto.email.email}") String email
) {}
