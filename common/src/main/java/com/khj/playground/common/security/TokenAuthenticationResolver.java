package com.khj.playground.common.security;

import org.springframework.security.core.Authentication;

/**
 * 토큰 문자열로 인증 정보를 얻기 위한 계약.
 * HTTP 필터와 WebSocket STOMP 인터셉터가 같은 판정 규칙을 쓰도록 auth 모듈이 구현한다.
 */
public interface TokenAuthenticationResolver {
  /**
   * @param bearerHeader "Bearer xxx" 형식. 유효하지 않으면 null 반환
   */
  Authentication resolve(String bearerHeader);
}
