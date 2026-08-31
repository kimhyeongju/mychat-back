package com.khj.mychatback.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * 클라이언트가 REST join API로 발급받은 memberId를 함께 실어 보낸다.
 * TODO: 현재는 memberId 소유권을 별도로 검증하지 않는다 (같은 방의 다른 memberId를 도용해 보낼 수 있음).
 *       추후 WebSocket 세션에 join 시점에 발급한 토큰을 바인딩하는 방식으로 강화 필요.
 */
public record ChatMessageSendRequest(
  @NotNull UUID memberId,
  @NotBlank @Size(max = 1000) String content
) {}
