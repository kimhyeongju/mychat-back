package com.khj.playground.common.security;

/**
 * 각 서비스 모듈이 자신의 인증 불필요 경로를 직접 등록한다.
 * auth 모듈이 다른 모듈의 URL을 알 필요가 없어진다.
 */
public interface PublicEndpoints {
  String[] patterns();
}
