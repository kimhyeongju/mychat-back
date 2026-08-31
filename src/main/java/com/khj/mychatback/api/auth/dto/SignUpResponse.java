package com.khj.mychatback.api.auth.dto;

import com.khj.mychatback.entity.jpa.User;
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
