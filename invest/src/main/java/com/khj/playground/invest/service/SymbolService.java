package com.khj.playground.invest.service;

import com.khj.playground.invest.entity.StockSymbol;
import com.khj.playground.invest.market.MarketDataClient;
import com.khj.playground.invest.market.MarketDataException;
import com.khj.playground.invest.market.SymbolInfo;
import com.khj.playground.invest.repository.StockSymbolRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 종목 검색과 이름 조회.
 * 검색 결과는 Redis 에 1시간, 알아낸 이름은 DB 에 영구 보관해 외부 호출을 아낀다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SymbolService {

  private static final Duration SEARCH_TTL = Duration.ofHours(1);
  private static final String SEARCH_PREFIX = "invest:search:";
  private static final String LINE = "\n";
  private static final String FIELD = "\t";

  private final MarketDataClient marketDataClient;
  private final MarketDataRateLimiter rateLimiter;
  private final StockSymbolRepository symbolRepository;
  private final StringRedisTemplate redisTemplate;
  private final KoreanSymbolSearch koreanSymbolSearch;

  @Transactional
  public List<SymbolInfo> search(String rawQuery) {
    String query = rawQuery == null ? "" : rawQuery.trim();
    if (query.length() < 1 || query.length() > 30) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "검색어는 1~30자여야 합니다."
      );
    }

    // 한글 검색어는 별칭 테이블에서만 찾는다. 외부 API 는 한글을 모르고, 호출 한도도 쓰지 않는다.
    if (KoreanSymbolSearch.containsKorean(query)) {
      return koreanSymbolSearch.search(query);
    }

    String key = SEARCH_PREFIX + query.toLowerCase(Locale.ROOT);

    Optional<List<SymbolInfo>> cached = readSearchCache(key);
    if (cached.isPresent()) {
      return cached.get();
    }

    if (!rateLimiter.tryAcquire()) {
      throw new ResponseStatusException(
        HttpStatus.TOO_MANY_REQUESTS,
        "검색 요청이 많습니다. 잠시 후 다시 시도해주세요."
      );
    }

    List<SymbolInfo> results;
    try {
      results = marketDataClient.search(query);
    } catch (MarketDataException e) {
      throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "종목 검색에 실패했습니다. 잠시 후 다시 시도해주세요."
      );
    }

    writeSearchCache(key, results);
    results.forEach(this::rememberName);
    return results;
  }

  /** 이름을 모르면 외부에서 한 번 받아 저장한다. 실패해도 심볼 자체를 이름으로 쓴다. */
  @Transactional
  public String resolveName(String symbol) {
    Optional<StockSymbol> saved = symbolRepository.findById(symbol);
    if (saved.isPresent()) {
      return saved.get().getName();
    }
    if (!rateLimiter.tryAcquire()) {
      return symbol;
    }
    try {
      Optional<SymbolInfo> profile = marketDataClient.fetchProfile(symbol);
      profile.ifPresent(this::rememberName);
      return profile.map(SymbolInfo::name).orElse(symbol);
    } catch (MarketDataException e) {
      log.warn("종목 이름 조회 실패 symbol={}", symbol);
      return symbol;
    }
  }

  /** DB 에 있는 이름만 돌려준다. 외부 호출 없이 목록을 그릴 때 쓴다. */
  @Transactional(readOnly = true)
  public String findNameOrSymbol(String symbol) {
    return symbolRepository
      .findById(symbol)
      .map(StockSymbol::getName)
      .or(() -> koreanSymbolSearch.findNameEn(symbol))
      .orElse(symbol);
  }

  private void rememberName(SymbolInfo info) {
    if (symbolRepository.existsById(info.symbol())) {
      return;
    }
    String name = info.name().length() > 200
      ? info.name().substring(0, 200)
      : info.name();
    symbolRepository.save(StockSymbol.of(info.symbol(), name, info.exchange()));
  }

  private Optional<List<SymbolInfo>> readSearchCache(String key) {
    try {
      String raw = redisTemplate.opsForValue().get(key);
      if (raw == null) {
        return Optional.empty();
      }
      if (raw.isEmpty()) {
        return Optional.of(List.of());
      }
      List<SymbolInfo> list = new ArrayList<>();
      for (String line : raw.split(LINE)) {
        String[] f = line.split(FIELD, -1);
        if (f.length == 3) {
          list.add(new SymbolInfo(f[0], f[1], f[2].isEmpty() ? null : f[2]));
        }
      }
      return Optional.of(list);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private void writeSearchCache(String key, List<SymbolInfo> results) {
    try {
      StringBuilder sb = new StringBuilder();
      for (SymbolInfo s : results) {
        if (sb.length() > 0) sb.append(LINE);
        sb
          .append(s.symbol())
          .append(FIELD)
          .append(s.name().replace(FIELD, " ").replace(LINE, " "))
          .append(FIELD)
          .append(s.exchange() == null ? "" : s.exchange());
      }
      redisTemplate.opsForValue().set(key, sb.toString(), SEARCH_TTL);
    } catch (Exception e) {
      log.warn("검색 캐시 쓰기 실패: {}", e.getMessage());
    }
  }
}
