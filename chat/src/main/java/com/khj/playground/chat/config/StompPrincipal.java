package com.khj.playground.chat.config;

import com.khj.playground.chat.enums.SenderType;
import java.security.Principal;
import java.util.UUID;

/**
 * WebSocket 세션에 묶이는 신원.
 * CONNECT 시점에 한 번 확정되고 이후 모든 프레임에서 재사용된다.
 */
public record StompPrincipal(UUID id, SenderType type, String nickname)
  implements Principal {
  @Override
  public String getName() {
    return type.name() + ":" + id;
  }
}
