package com.khj.mychatback.api.auth.service;

import com.khj.mychatback.config.jwt.JwtTokenProvider;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * key: refresh:{username} → value: 발급된 refresh token
 * 로그인 시마다 갱신되므로, 같은 계정으로 재로그인하면 이전 refresh token은 자동으로 무효화된다(단일 세션).
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final String KEY_PREFIX = "refresh:";

  private final StringRedisTemplate redisTemplate;
  private final JwtTokenProvider jwtTokenProvider;

  public void save(String username, String refreshToken) {
    Duration ttl = Duration.ofMillis(
      jwtTokenProvider.getRefreshTokenExpirationMs()
    );
    redisTemplate.opsForValue().set(KEY_PREFIX + username, refreshToken, ttl);
  }

  /** 전달받은 refresh token이 저장된 값과 일치하는지 확인 (탈취된 구 토큰 재사용 방지) */
  public boolean isValid(String username, String refreshToken) {
    String saved = redisTemplate.opsForValue().get(KEY_PREFIX + username);
    return saved != null && saved.equals(refreshToken);
  }

  public void delete(String username) {
    redisTemplate.delete(KEY_PREFIX + username);
  }
}
