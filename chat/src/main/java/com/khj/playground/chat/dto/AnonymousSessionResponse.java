package com.khj.playground.chat.dto;

import com.khj.playground.chat.entity.AnonymousSession;
import java.util.UUID;

public record AnonymousSessionResponse(UUID anonymousId, String nickname) {
  public static AnonymousSessionResponse from(AnonymousSession session) {
    return new AnonymousSessionResponse(session.getId(), session.getNickname());
  }
}
