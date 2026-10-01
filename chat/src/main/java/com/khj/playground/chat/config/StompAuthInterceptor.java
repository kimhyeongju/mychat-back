package com.khj.playground.chat.config;

import com.khj.playground.chat.service.ChatSenderResolver;
import com.khj.playground.common.security.TokenAuthenticationResolver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * STOMP CONNECT 프레임에서 신원을 확인한다.
 *
 * HTTP 요청은 Security 필터 체인이 처리하지만, 핸드셰이크 이후의 STOMP 프레임은
 * 필터를 거치지 않는다. 그래서 인증을 여기서 직접 수행해야 하며,
 * 이 과정을 생략하면 누구나 임의의 신원으로 메시지를 보낼 수 있게 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

  private final ChatSenderResolver senderResolver;
  private final TokenAuthenticationResolver tokenResolver;

  @Override
  public Message<?> preSend(
    @NonNull Message<?> message,
    @NonNull MessageChannel channel
  ) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
      message,
      StompHeaderAccessor.class
    );

    if (
      accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())
    ) {
      return message;
    }

    Authentication authentication = tokenResolver.resolve(
      accessor.getFirstNativeHeader("Authorization")
    );
    UUID anonymousId = parseUuid(
      accessor.getFirstNativeHeader("X-Anonymous-Id")
    );

    try {
      var sender = senderResolver.resolve(authentication, anonymousId);
      accessor.setUser(
        new StompPrincipal(sender.id(), sender.type(), sender.nickname())
      );
    } catch (ResponseStatusException e) {
      // 신원을 확인할 수 없으면 연결 자체를 거부한다.
      log.warn("ws_auth_denied reason={}", e.getReason());
      throw new IllegalArgumentException("인증에 실패했습니다.");
    }

    return message;
  }

  private UUID parseUuid(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
