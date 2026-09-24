package com.khj.playground.auth.dto;

import com.khj.playground.auth.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 로그인한 본인의 정보. 비밀번호는 절대 포함하지 않는다.
 * 휴대폰 번호는 본인 확인용으로만 쓰이므로 마스킹해서 내린다.
 */
public record MeResponse(
  UUID id,
  String username,
  String nickname,
  String email,
  String phoneNumber,
  LocalDateTime createdAt
) {
  public static MeResponse from(User user) {
    return new MeResponse(
      user.getId(),
      user.getUsername(),
      user.getNickname(),
      user.getEmail(),
      maskPhoneNumber(user.getPhoneNumber()),
      user.getCreatedAt()
    );
  }

  /** 01012345678 -> 010****5678 */
  private static String maskPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.length() < 7) {
      return phoneNumber;
    }
    int tailStart = phoneNumber.length() - 4;
    return (
      phoneNumber.substring(0, 3) +
      "*".repeat(tailStart - 3) +
      phoneNumber.substring(tailStart)
    );
  }
}
