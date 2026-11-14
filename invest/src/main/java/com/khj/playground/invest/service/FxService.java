package com.khj.playground.invest.service;

import com.khj.playground.invest.dto.FxResponse;
import com.khj.playground.invest.market.FxRateClient;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * USD→KRW 환율. 화면 환산 전용이고 장부에는 쓰지 않는다.
 * 6시간마다 갱신하고, 외부 조회에 실패하면 마지막 값을, 그것도 없으면 고정 추정치를 돌려준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FxService {

  private static final String KEY = "invest:fx:usdkrw";
  private static final Duration REFRESH_AFTER = Duration.ofHours(6);
  private static final Duration KEEP = Duration.ofDays(7);
  /** 외부 조회가 한 번도 성공하지 못했을 때의 추정치. 화면에 '추정'으로 표시된다. */
  private static final BigDecimal FALLBACK_RATE = new BigDecimal("1400");

  private final FxRateClient fxRateClient;
  private final StringRedisTemplate redisTemplate;

  public FxResponse getUsdKrw() {
    Optional<Cached> cached = read();
    if (cached.isPresent() && cached.get().isFresh()) {
      return cached.get().toResponse("live");
    }

    Optional<BigDecimal> fetched = fxRateClient.fetchUsdKrw();
    if (fetched.isPresent()) {
      Cached fresh = new Cached(fetched.get(), Instant.now());
      write(fresh);
      return fresh.toResponse("live");
    }

    return cached
      .map(c -> c.toResponse("stale"))
      .orElseGet(() ->
        new FxResponse("USD", "KRW", FALLBACK_RATE, null, "fallback")
      );
  }

  private Optional<Cached> read() {
    try {
      String raw = redisTemplate.opsForValue().get(KEY);
      if (raw == null) return Optional.empty();
      String[] parts = raw.split("\\|");
      return Optional.of(
        new Cached(
          new BigDecimal(parts[0]),
          Instant.ofEpochMilli(Long.parseLong(parts[1]))
        )
      );
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private void write(Cached cached) {
    try {
      redisTemplate
        .opsForValue()
        .set(
          KEY,
          cached.rate().toPlainString() + "|" + cached.at().toEpochMilli(),
          KEEP
        );
    } catch (Exception e) {
      log.warn("환율 캐시 쓰기 실패: {}", e.getMessage());
    }
  }

  private record Cached(BigDecimal rate, Instant at) {
    boolean isFresh() {
      return Duration.between(at, Instant.now()).compareTo(REFRESH_AFTER) < 0;
    }

    FxResponse toResponse(String source) {
      return new FxResponse("USD", "KRW", rate, at, source);
    }
  }
}
