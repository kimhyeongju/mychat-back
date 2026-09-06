package com.khj.playground.api.user.dto;

import com.khj.playground.entity.jpa.User;
import java.util.UUID;

public record UserSummaryResponse(UUID userId, String nickname) {
  public static UserSummaryResponse from(User user) {
    return new UserSummaryResponse(user.getId(), user.getNickname());
  }
}
