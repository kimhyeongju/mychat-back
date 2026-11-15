package com.khj.playground.auth.dto;

import com.khj.playground.auth.entity.User;
import com.khj.playground.auth.enums.Role;
import java.time.LocalDateTime;
import java.util.UUID;

/** 운영자 화면용 회원 정보. 비밀번호는 포함하지 않는다. 전화번호는 마스킹하지 않는다. */
public record AdminUserResponse(
  UUID id,
  String username,
  String nickname,
  String phoneNumber,
  String email,
  boolean admin,
  LocalDateTime createdAt
) {
  public static AdminUserResponse from(User user) {
    return new AdminUserResponse(
      user.getId(),
      user.getUsername(),
      user.getNickname(),
      user.getPhoneNumber(),
      user.getEmail(),
      user.getRole() == Role.ADMIN,
      user.getCreatedAt()
    );
  }
}
