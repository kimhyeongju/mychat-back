package com.khj.playground.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 부분 수정. null인 필드는 변경하지 않는다.
 * 비밀번호와 휴대폰 번호는 별도 인증이 필요하므로 여기서 다루지 않는다.
 */
public record UpdateProfileRequest(
  // SignUpRequest의 닉네임 제약과 동일하게 유지한다.
  @Size(min = 2, max = 12) String nickname,

  @Email String email
) {}
