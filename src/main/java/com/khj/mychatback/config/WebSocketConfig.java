package com.khj.mychatback.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 클라이언트는 /ws 로 SockJS 연결 후, /topic/rooms/{roomId} 를 구독해 실시간 메시지를 받는다.
 * 메시지 전송은 /app/rooms/{roomId}/send 로 보내면 ChatStompController가 처리한다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
      .addEndpoint("/ws")
      .setAllowedOriginPatterns(
        "http://localhost:5173",
        "http://192.168.*.*:5173",
        "http://10.*.*.*:5173"
      )
      .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }
}
