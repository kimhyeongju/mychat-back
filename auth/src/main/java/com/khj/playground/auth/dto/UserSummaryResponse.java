package com.khj.playground.auth.dto;

import com.khj.playground.auth.entity.User;
import java.util.UUID;

public record UserSummaryResponse(UUID userId, String nickname) {
  public static UserSummaryResponse from(User user) {
    return new UserSummaryResponse(user.getId(), user.getNickname());
  }
}
