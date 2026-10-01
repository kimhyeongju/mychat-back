package com.khj.playground.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/** 연결/해제/구독 이벤트를 남긴다. 이상 탐지의 기초 데이터가 된다. */
@Slf4j
@Component
public class ChatWebSocketEventListener {

  @EventListener
  public void onConnected(SessionConnectedEvent event) {
    var accessor = StompHeaderAccessor.wrap(event.getMessage());
    log.info(
      "ws_connect sessionId={} principal={}",
      accessor.getSessionId(),
      event.getUser() != null ? event.getUser().getName() : "unknown"
    );
  }

  @EventListener
  public void onSubscribe(SessionSubscribeEvent event) {
    var accessor = StompHeaderAccessor.wrap(event.getMessage());
    log.info(
      "ws_subscribe sessionId={} destination={} principal={}",
      accessor.getSessionId(),
      accessor.getDestination(),
      event.getUser() != null ? event.getUser().getName() : "unknown"
    );
  }

  @EventListener
  public void onDisconnect(SessionDisconnectEvent event) {
    log.info(
      "ws_disconnect sessionId={} status={}",
      event.getSessionId(),
      event.getCloseStatus()
    );
  }
}
