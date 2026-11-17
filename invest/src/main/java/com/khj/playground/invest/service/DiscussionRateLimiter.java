package com.khj.playground.invest.service;

import com.khj.playground.common.exception.ApiException;
import com.khj.playground.invest.exception.InvestErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 토론 글·댓글 도배 방지. 사용자별로 1분에 쓸 수 있는 개수를 제한한다. */
@Slf4j
@Component
public class DiscussionRateLimiter {

  static final int POSTS_PER_MINUTE = 3;
  static final int COMMENTS_PER_MINUTE = 10;

  private final StringRedisTemplate redisTemplate;

  public DiscussionRateLimiter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void checkPost(UUID userId) {
    check("post", userId, POSTS_PER_MINUTE);
  }

  public void checkComment(UUID userId) {
    check("comment", userId, COMMENTS_PER_MINUTE);
  }

  /** Redis 장애 시에는 막지 않는다(글쓰기 자체가 더 중요). */
  private void check(String action, UUID userId, int limit) {
    String key =
      "invest:discussion:" + action + ":" + userId + ":" + (Instant.now().getEpochSecond() / 60);
    try {
      Long count = redisTemplate.opsForValue().increment(key);
      if (count != null && count == 1L) {
        redisTemplate.expire(key, Duration.ofMinutes(2));
      }
      if (count != null && count > limit) {
        throw new ApiException(InvestErrorCode.DISCUSSION_RATE_LIMITED);
      }
    } catch (ApiException e) {
      throw e;
    } catch (RuntimeException e) {
      log.warn("토론 도배 방지 카운터 오류: {}", e.getMessage());
    }
  }
}
