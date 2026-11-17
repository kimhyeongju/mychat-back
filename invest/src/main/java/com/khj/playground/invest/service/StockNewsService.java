package com.khj.playground.invest.service;

import com.khj.playground.common.exception.ApiException;
import com.khj.playground.invest.dto.NewsItemResponse;
import com.khj.playground.invest.exception.InvestErrorCode;
import com.khj.playground.invest.market.MarketDataClient;
import com.khj.playground.invest.market.MarketDataException;
import com.khj.playground.invest.market.NewsItem;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 종목 뉴스. 같은 종목은 일정 시간 캐시해서 외부 호출 한도(분당)를 아낀다.
 * 제공자가 실패하거나 한도에 걸리면 오래된 캐시라도 돌려주고, 캐시가 없을 때만 오류를 낸다.
 */
@Slf4j
@Service
public class StockNewsService {

  static final int MAX_ITEMS = 15;
  private static final int MAX_CACHED_SYMBOLS = 500;
  private static final int MAX_SUMMARY_LENGTH = 300;
  private static final Pattern TAGS = Pattern.compile("<[^>]*>");
  private static final Pattern SPACES = Pattern.compile("\\s+");

  private final MarketDataClient marketDataClient;
  private final MarketDataRateLimiter rateLimiter;
  private final PriceService priceService;
  private final Duration ttl;
  private final Map<String, Cached> cache = new ConcurrentHashMap<>();

  public StockNewsService(
    MarketDataClient marketDataClient,
    MarketDataRateLimiter rateLimiter,
    PriceService priceService,
    @Value("${invest.news.cache-seconds:300}") long cacheSeconds
  ) {
    this.marketDataClient = marketDataClient;
    this.rateLimiter = rateLimiter;
    this.priceService = priceService;
    this.ttl = Duration.ofSeconds(cacheSeconds);
  }

  public List<NewsItemResponse> getNews(String rawSymbol) {
    String symbol = priceService.normalize(rawSymbol);
    Cached cached = cache.get(symbol);
    if (cached != null && cached.isFresh(ttl)) {
      return cached.items();
    }
    if (!rateLimiter.tryAcquire()) {
      return fallback(cached);
    }
    try {
      List<NewsItemResponse> items = marketDataClient
        .fetchNews(symbol, MAX_ITEMS)
        .stream()
        .map(StockNewsService::toResponse)
        .sorted(
          Comparator.comparing(
            NewsItemResponse::createdAt,
            Comparator.nullsLast(Comparator.reverseOrder())
          )
        )
        .toList();
      remember(symbol, items);
      return items;
    } catch (MarketDataException e) {
      log.warn("뉴스 조회 실패 symbol={} rateLimited={}", symbol, e.isRateLimited());
      return fallback(cached);
    }
  }

  private List<NewsItemResponse> fallback(Cached cached) {
    if (cached != null) {
      return cached.items();
    }
    throw new ApiException(InvestErrorCode.NEWS_UNAVAILABLE);
  }

  private void remember(String symbol, List<NewsItemResponse> items) {
    if (cache.size() >= MAX_CACHED_SYMBOLS) {
      // 가장 오래된 항목부터 비운다. 종목 수가 한정적이라 단순한 방식으로 충분하다.
      cache
        .entrySet()
        .stream()
        .min(Comparator.comparing(e -> e.getValue().loadedAt()))
        .ifPresent(e -> cache.remove(e.getKey()));
    }
    cache.put(symbol, new Cached(items, Instant.now()));
  }

  static NewsItemResponse toResponse(NewsItem item) {
    return new NewsItemResponse(
      item.id(),
      item.headline().strip(),
      cleanSummary(item.summary()),
      item.source(),
      safeUrl(item.url()),
      item.createdAt()
    );
  }

  /** 요약에 HTML 이 섞여 올 수 있어 태그를 걷고 길이를 제한한다. 화면은 텍스트로만 그린다. */
  static String cleanSummary(String summary) {
    if (summary == null) return null;
    String text = SPACES.matcher(TAGS.matcher(summary).replaceAll(" ")).replaceAll(" ").strip();
    if (text.isEmpty()) return null;
    return text.length() > MAX_SUMMARY_LENGTH
      ? text.substring(0, MAX_SUMMARY_LENGTH).strip() + "…"
      : text;
  }

  /** javascript: 같은 스킴이 링크로 나가지 않도록 http(s) 만 허용한다. */
  static String safeUrl(String url) {
    if (url == null) return null;
    String lower = url.strip().toLowerCase();
    return lower.startsWith("https://") || lower.startsWith("http://") ? url.strip() : null;
  }

  private record Cached(List<NewsItemResponse> items, Instant loadedAt) {
    boolean isFresh(Duration ttl) {
      return Duration.between(loadedAt, Instant.now()).compareTo(ttl) < 0;
    }
  }
}
