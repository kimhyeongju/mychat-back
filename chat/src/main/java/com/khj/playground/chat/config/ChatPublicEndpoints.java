package com.khj.playground.chat.config;

import com.khj.playground.common.security.PublicEndpoint;
import com.khj.playground.common.security.PublicEndpoints;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * 오픈 채팅은 익명 접근을 허용하므로 조회와 메시지 전송 모두 공개한다.
 * DM 경로(/api/chat/dm/**)는 등록하지 않아 자동으로 인증이 필요해진다.
 */
@Component
public class ChatPublicEndpoints implements PublicEndpoints {

  @Override
  public List<PublicEndpoint> endpoints() {
    return List.of(
      PublicEndpoint.any("/ws/**"),
      PublicEndpoint.get("/api/chat/rooms/open"),
      PublicEndpoint.get("/api/chat/rooms/open/*"),
      PublicEndpoint.get("/api/chat/rooms/open/*/messages"),
      // 익명 참여를 위해 세션 발급과 오픈 채팅 전송은 공개한다.
      new PublicEndpoint(HttpMethod.POST, "/api/chat/anonymous-session"),
      new PublicEndpoint(HttpMethod.POST, "/api/chat/rooms/open/*/messages")
    );
  }
}
