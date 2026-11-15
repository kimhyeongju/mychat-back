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
  @NotBlank
  @Size(min = 4, max = 20)
  @Pattern(
    regexp = "^[a-zA-Z0-9_]+$",
    message = "아이디는 영문/숫자/언더스코어만 사용할 수 있습니다."
  )
  String username,

  @NotBlank @Size(min = 1, max = 72) String password,

  @NotBlank @Size(min = 2, max = 12) String nickname,

  @Pattern(regexp = "^[0-9]{8,20}$", message = "전화번호는 숫자 8~20자리여야 합니다.")
  String phoneNumber,

  @Email String email
) {}
