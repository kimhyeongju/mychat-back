package com.khj.playground.invest.service;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 외부 시세 API 호출 횟수를 서버 전체에서 분당 N회로 묶는다.
 * Finnhub 무료 티어가 분당 60회라 여유를 두고 50회로 잡는다.
 * Redis 분 단위 카운터를 쓰므로 인스턴스가 늘어나도 합산 한도가 유지된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataRateLimiter {

  private static final int CALLS_PER_MINUTE = 50;
  private static final String KEY_PREFIX = "invest:market:calls:";

  private final StringRedisTemplate redisTemplate;

  /** @return 호출해도 되면 true. Redis 장애 시에는 호출을 허용한다(시세가 더 중요). */
  public boolean tryAcquire() {
    String key = KEY_PREFIX + (Instant.now().getEpochSecond() / 60);
    try {
      Long count = redisTemplate.opsForValue().increment(key);
      if (count != null && count == 1L) {
        redisTemplate.expire(key, Duration.ofMinutes(2));
      }
      return count == null || count <= CALLS_PER_MINUTE;
    } catch (Exception e) {
      log.warn("호출 제한 카운터 실패, 이번 호출은 허용: {}", e.getMessage());
      return true;
    }
  }
}
