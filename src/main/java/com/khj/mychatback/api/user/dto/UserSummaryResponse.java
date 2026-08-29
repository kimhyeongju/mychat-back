package com.khj.mychatback.api.user.dto;

import com.khj.mychatback.entity.jpa.User;

public record UserSummaryResponse(Long userId, String nickname) {
  public static UserSummaryResponse from(User user) {
    return new UserSummaryResponse(user.getId(), user.getNickname());
  }
}
