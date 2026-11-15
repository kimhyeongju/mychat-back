package com.khj.playground.invest.service;

import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 외부 시세 API 호출 횟수를 서버 전체에서 분당 N회로 묶는다.
 * 기본값 50은 Finnhub 무료 티어(분당 60회) 기준이다. Alpaca 는 분당 200회라 설정으로 올려 쓴다.
 * Redis 분 단위 카운터를 쓰므로 인스턴스가 늘어나도 합산 한도가 유지된다.
 */
@Slf4j
@Component
public class MarketDataRateLimiter {

  private static final String KEY_PREFIX = "invest:market:calls:";

  private final StringRedisTemplate redisTemplate;
  private final int callsPerMinute;

  public MarketDataRateLimiter(
    StringRedisTemplate redisTemplate,
    @Value("${invest.market-data.calls-per-minute:50}") int callsPerMinute
  ) {
    this.redisTemplate = redisTemplate;
    this.callsPerMinute = callsPerMinute;
  }

  /** @return 호출해도 되면 true. Redis 장애 시에는 호출을 허용한다(시세가 더 중요). */
  public boolean tryAcquire() {
    String key = KEY_PREFIX + (Instant.now().getEpochSecond() / 60);
    try {
      Long count = redisTemplate.opsForValue().increment(key);
      if (count != null && count == 1L) {
        redisTemplate.expire(key, Duration.ofMinutes(2));
      }
      return count == null || count <= callsPerMinute;
    } catch (Exception e) {
      log.warn("호출 제한 카운터 실패, 이번 호출은 허용: {}", e.getMessage());
      return true;
    }
  }
}
