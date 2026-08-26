package com.khj.mychatback.api.auth.dto;

import com.khj.mychatback.entity.jpa.User;

public record SignUpResponse(
        Long id,
        String username,
        String nickname
) {
    public static SignUpResponse from(User user) {
        return new SignUpResponse(user.getId(), user.getUsername(), user.getNickname());
    }
}
