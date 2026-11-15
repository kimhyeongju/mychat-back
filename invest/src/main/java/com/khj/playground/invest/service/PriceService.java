package com.khj.playground.invest.service;

import com.khj.playground.invest.market.MarketClock;
import com.khj.playground.invest.market.MarketDataClient;
import com.khj.playground.invest.market.MarketDataException;
import com.khj.playground.invest.market.Quote;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 시세 조회의 단일 창구. 캐시 → 호출 제한 → 외부 호출 순으로 처리한다.
 *
 * Redis 키
 * - invest:quote:{symbol}         마지막으로 받은 시세. 24시간 보관해 외부 장애 시 폴백으로 쓴다.
 * - invest:quote:missing:{symbol} 존재하지 않는 심볼 표시. 같은 오타로 반복 호출되는 것을 막는다.
 *
 * "신선함"은 TTL 이 아니라 fetchedAt 으로 판단한다. 정규장 15초, 시간외 60초, 그 외 10분이 지나면 다시 받는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

  private static final Pattern SYMBOL = Pattern.compile("^[A-Z][A-Z0-9.\\-]{0,9}$");
  private static final Duration FRESH_WHILE_OPEN = Duration.ofSeconds(15);
  private static final Duration FRESH_WHILE_EXTENDED = Duration.ofSeconds(60);
  private static final Duration FRESH_WHILE_CLOSED = Duration.ofMinutes(10);
  private static final Duration VALUATION_GRACE = Duration.ofMinutes(2);
  private static final int BATCH_CHUNK = 50;
  private static final Duration KEEP = Duration.ofHours(24);
  private static final Duration MISSING_TTL = Duration.ofHours(1);
  private static final String QUOTE_PREFIX = "invest:quote:";
  private static final String MISSING_PREFIX = "invest:quote:missing:";

  private final MarketDataClient marketDataClient;
  private final MarketDataRateLimiter rateLimiter;
  private final MarketClock marketClock;
  private final StringRedisTemplate redisTemplate;

  /** 대소문자를 통일하고 허용 문자만 받는다. 외부 API 에 임의 문자열이 그대로 나가지 않게 한다. */
  public String normalize(String rawSymbol) {
    if (rawSymbol == null) {
      throw badSymbol();
    }
    String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
    if (!SYMBOL.matcher(symbol).matches()) {
      throw badSymbol();
    }
    return symbol;
  }

  /**
   * 화면 표시용. 최신 시세를 받되 실패하면 오래된 캐시라도 돌려준다.
   *
   * @throws ResponseStatusException 404 없는 심볼, 503 시세를 전혀 구할 수 없음
   */
  public QuoteView getQuote(String symbol) {
    if (isKnownMissing(symbol)) {
      throw notFound();
    }

    Optional<Quote> cached = readCache(symbol);
    if (cached.isPresent() && isFresh(cached.get())) {
      return new QuoteView(cached.get(), false);
    }

    if (!rateLimiter.tryAcquire()) {
      return cached
        .map(q -> new QuoteView(q, true))
        .orElseThrow(this::unavailable);
    }

    try {
      Optional<Quote> fetched = marketDataClient.fetchQuote(symbol);
      if (fetched.isEmpty()) {
        markMissing(symbol);
        throw notFound();
      }
      writeCache(fetched.get());
      return new QuoteView(fetched.get(), false);
    } catch (MarketDataException e) {
      log.warn("시세 조회 실패 symbol={} rateLimited={}", symbol, e.isRateLimited());
      return cached
        .map(q -> new QuoteView(q, true))
        .orElseThrow(this::unavailable);
    }
  }

  /** 예외를 던지지 않는 버전. 랭킹처럼 한 종목 실패가 전체를 막으면 안 되는 곳에서 쓴다. */
  public Optional<QuoteView> findQuote(String symbol) {
    try {
      return Optional.of(getQuote(symbol));
    } catch (ResponseStatusException e) {
      return Optional.empty();
    }
  }

  /**
   * 평가용(포트폴리오·랭킹). 캐시가 2분 이내면 신선하지 않아도 그대로 쓴다.
   * 보유 종목은 HeldSymbolRefresher 가 따로 갱신하므로, 평가를 그릴 때마다 종목별로
   * 외부 호출이 나가 호출 한도를 태우는 일을 막는다.
   */
  public Optional<QuoteView> findQuoteForValuation(String symbol) {
    Optional<Quote> cached = readCache(symbol);
    if (cached.isPresent()) {
      Duration age = Duration.between(cached.get().fetchedAt(), Instant.now());
      if (isFresh(cached.get()) || age.compareTo(VALUATION_GRACE) < 0) {
        return Optional.of(new QuoteView(cached.get(), false));
      }
    }
    return findQuote(symbol);
  }

  /** 주문 체결용. 1시간 넘게 갱신되지 않은 시세로는 체결하지 않는다. */
  public Quote getQuoteForTrade(String symbol) {
    QuoteView view = getQuote(symbol);
    Duration age = Duration.between(view.quote().fetchedAt(), Instant.now());
    if (age.compareTo(Duration.ofHours(1)) > 0) {
      throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "최신 시세를 가져올 수 없어 주문을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."
      );
    }
    return view.quote();
  }

  public boolean isMarketOpen() {
    return marketClock.isRegularSessionOpen();
  }

  /** 보유 종목 주기 갱신에서 쓴다. 신선하면 외부 호출을 하지 않는다. */
  boolean refreshIfStale(String symbol) {
    Optional<Quote> cached = readCache(symbol);
    if (cached.isPresent() && isFresh(cached.get())) {
      return false;
    }
    findQuote(symbol);
    return true;
  }

  /**
   * 오래된 종목만 골라 일괄 조회로 갱신한다. 호출은 한 묶음(최대 50종목)에 한도 1회만 쓴다.
   *
   * @return 새로 받아 캐시에 쓴 종목 수
   */
  int refreshBatch(List<String> symbols) {
    List<String> stale = new ArrayList<>();
    for (String symbol : symbols) {
      if (isKnownMissing(symbol)) continue;
      Optional<Quote> cached = readCache(symbol);
      if (cached.isEmpty() || !isFresh(cached.get())) {
        stale.add(symbol);
      }
    }
    if (stale.isEmpty()) {
      return 0;
    }

    int refreshed = 0;
    for (int i = 0; i < stale.size(); i += BATCH_CHUNK) {
      List<String> chunk = stale.subList(i, Math.min(i + BATCH_CHUNK, stale.size()));
      if (!rateLimiter.tryAcquire()) {
        break;
      }
      try {
        Map<String, Quote> fetched = marketDataClient.fetchQuotes(chunk);
        fetched.values().forEach(this::writeCache);
        refreshed += fetched.size();
      } catch (MarketDataException e) {
        log.warn("일괄 시세 갱신 실패 rateLimited={}", e.isRateLimited());
        break;
      }
    }
    return refreshed;
  }

  boolean supportsBatch() {
    return marketDataClient.supportsBatch();
  }

  private boolean isFresh(Quote quote) {
    Duration window = marketClock.isRegularSessionOpen()
      ? FRESH_WHILE_OPEN
      : marketClock.isExtendedSessionOpen()
        ? FRESH_WHILE_EXTENDED
        : FRESH_WHILE_CLOSED;
    return Duration
      .between(quote.fetchedAt(), Instant.now())
      .compareTo(window) <
    0;
  }

  private Optional<Quote> readCache(String symbol) {
    try {
      String raw = redisTemplate.opsForValue().get(QUOTE_PREFIX + symbol);
      return raw == null
        ? Optional.empty()
        : Optional.of(Quote.deserialize(symbol, raw));
    } catch (Exception e) {
      log.warn("시세 캐시 읽기 실패 symbol={}: {}", symbol, e.getMessage());
      return Optional.empty();
    }
  }

  private void writeCache(Quote quote) {
    try {
      redisTemplate
        .opsForValue()
        .set(QUOTE_PREFIX + quote.symbol(), quote.serialize(), KEEP);
    } catch (Exception e) {
      log.warn("시세 캐시 쓰기 실패 symbol={}: {}", quote.symbol(), e.getMessage());
    }
  }

  private boolean isKnownMissing(String symbol) {
    try {
      return Boolean.TRUE.equals(redisTemplate.hasKey(MISSING_PREFIX + symbol));
    } catch (Exception e) {
      return false;
    }
  }

  private void markMissing(String symbol) {
    try {
      redisTemplate
        .opsForValue()
        .set(MISSING_PREFIX + symbol, "1", MISSING_TTL);
    } catch (Exception e) {
      log.warn("missing 표시 실패 symbol={}: {}", symbol, e.getMessage());
    }
  }

  private ResponseStatusException badSymbol() {
    return new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "종목 코드 형식이 올바르지 않습니다."
    );
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "존재하지 않는 종목입니다."
    );
  }

  private ResponseStatusException unavailable() {
    return new ResponseStatusException(
      HttpStatus.SERVICE_UNAVAILABLE,
      "시세를 가져올 수 없습니다. 잠시 후 다시 시도해주세요."
    );
  }

  /** @param stale 최신 조회에 실패해 이전 값을 돌려준 경우 true */
  public record QuoteView(Quote quote, boolean stale) {}
}
