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

    if (accessor == null) {
      return message;
    }

    StompCommand command = accessor.getCommand();

    // 클라이언트가 브로커 목적지(/topic, /queue, /user)로 직접 보내면
    // 컨트롤러의 저장·속도 제한·발신자 확정을 모두 건너뛴다.
    if (StompCommand.SEND.equals(command)) {
      String destination = accessor.getDestination();
      if (destination == null || !destination.startsWith("/app/")) {
        log.warn("ws_send_denied destination={}", destination);
        throw new IllegalArgumentException("허용되지 않는 전송 경로입니다.");
      }
      return message;
    }

    if (StompCommand.SUBSCRIBE.equals(command)) {
      String destination = accessor.getDestination();
      boolean allowed =
        destination != null &&
        (
          destination.startsWith("/topic/rooms/") ||
          destination.startsWith("/user/queue/dm/")
        );
      if (!allowed) {
        log.warn("ws_subscribe_denied destination={}", destination);
        throw new IllegalArgumentException("허용되지 않는 구독 경로입니다.");
      }
      return message;
    }

    if (!StompCommand.CONNECT.equals(command)) {
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
