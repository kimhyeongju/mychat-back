package com.khj.playground.auth.dto;

import com.khj.playground.auth.entity.User;
import java.util.UUID;

public record SignUpResponse(UUID id, String username, String nickname) {
  public static SignUpResponse from(User user) {
    return new SignUpResponse(
      user.getId(),
      user.getUsername(),
      user.getNickname()
    );
  }
}
