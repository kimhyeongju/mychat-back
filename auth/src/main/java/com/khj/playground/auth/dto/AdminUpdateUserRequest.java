package com.khj.playground.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 값이 null 이면 바꾸지 않는다. 권한(role)은 이 화면에서 바꿀 수 없다.
 * 비밀번호를 주면 규칙 없이 재설정하고 기존 로그인 세션(리프레시 토큰)을 끊는다.
 */
public record AdminUpdateUserRequest(
  @Size(min = 2, max = 12) String nickname,

  @Pattern(regexp = "^[0-9]{8,20}$", message = "전화번호는 숫자 8~20자리여야 합니다.")
  String phoneNumber,

  @Email String email,

  @Size(min = 1, max = 72) String password
) {}
