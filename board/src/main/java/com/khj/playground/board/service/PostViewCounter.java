package com.khj.playground.board.service;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 같은 사용자가 짧은 시간에 반복 조회해도 조회수가 오르지 않도록 막는다.
 * Redis에 "이 글을 이 사람이 봤음" 표시를 TTL과 함께 남기고,
 * 이미 표시가 있으면 조회수를 올리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostViewCounter {

  /** 같은 사람이 이 시간 안에 다시 보면 조회수가 오르지 않는다. */
  private static final Duration COOLDOWN = Duration.ofHours(1);

  private final StringRedisTemplate redisTemplate;

  /**
   * @return 조회수를 올려야 하면 true
   */
  public boolean markViewed(UUID postId, String viewerKey) {
    String key = "board:post:%s:viewer:%s".formatted(postId, viewerKey);

    try {
      // setIfAbsent는 키가 없을 때만 저장하고 true를 반환한다 (SET NX EX).
      // 조회와 저장이 한 번의 원자적 명령이라 동시 요청에도 중복 증가가 없다.
      Boolean firstView = redisTemplate
        .opsForValue()
        .setIfAbsent(key, "1", COOLDOWN);

      return Boolean.TRUE.equals(firstView);
    } catch (Exception e) {
      // Redis가 죽어도 글 조회 자체는 되어야 한다. 조회수만 포기한다.
      log.warn(
        "조회수 중복 검사 실패, 이번 조회는 집계하지 않음: {}",
        e.getMessage()
      );
      return false;
    }
  }
}
