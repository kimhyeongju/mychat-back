package com.khj.playground.common.security;

import org.springframework.http.HttpMethod;

/**
 * 인증 없이 접근 가능한 엔드포인트 하나.
 * method가 null이면 모든 메서드를 허용한다.
 */
public record PublicEndpoint(HttpMethod method, String pattern) {
  public static PublicEndpoint any(String pattern) {
    return new PublicEndpoint(null, pattern);
  }

  public static PublicEndpoint get(String pattern) {
    return new PublicEndpoint(HttpMethod.GET, pattern);
  }
}