package com.khj.playground.invest.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Alpaca 무료 시세(IEX).
 * - 시세: 스냅샷 API 로 여러 종목을 한 번에 받는다. 분당 200회 제한.
 * - 종목 검색: 거래 가능한 종목 목록(/v2/assets)을 통째로 받아 메모리에서 검색한다.
 *   그래서 검색은 외부 호출이 없고, 하루에 한 번만 목록을 갱신한다.
 * - 무료 실시간 데이터는 IEX 한 거래소 체결만 반영한다. 시간외나 체결이 드문 종목은
 *   마지막 체결 시각(tradedAt)이 오래될 수 있다.
 */
@Slf4j
@Component
@ConditionalOnProperty(
  name = "invest.market-data.provider",
  havingValue = "alpaca"
)
public class AlpacaMarketDataClient implements MarketDataClient {

  private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
  private static final int BATCH_SIZE = 50;
  private static final Duration ASSET_TTL = Duration.ofHours(24);
  private static final int MAX_RESULTS = 20;
  /** 장외(OTC) 종목은 제외한다. */
  private static final Set<String> EXCLUDED_EXCHANGES = Set.of("OTC");

  private final RestClient dataClient;
  private final RestClient tradingClient;
  private final String feed;

  private volatile AssetIndex assetIndex;

  public AlpacaMarketDataClient(
    @Value("${invest.market-data.alpaca.key-id}") String keyId,
    @Value("${invest.market-data.alpaca.secret}") String secret,
    @Value("${invest.market-data.alpaca.data-url:https://data.alpaca.markets}") String dataUrl,
    @Value("${invest.market-data.alpaca.trading-url:https://paper-api.alpaca.markets}") String tradingUrl,
    @Value("${invest.market-data.alpaca.feed:iex}") String feed
  ) {
    if (keyId == null || keyId.isBlank() || secret == null || secret.isBlank()) {
      throw new IllegalStateException(
        "invest.market-data.provider=alpaca 이면 ALPACA_API_KEY_ID, ALPACA_API_SECRET_KEY 가 필요합니다."
      );
    }
    this.feed = feed;
    this.dataClient = build(dataUrl, keyId, secret, Duration.ofSeconds(5));
    // 종목 목록은 용량이 커서 읽기 제한을 넉넉히 둔다.
    this.tradingClient = build(tradingUrl, keyId, secret, Duration.ofSeconds(20));
  }

  private static RestClient build(
    String baseUrl,
    String keyId,
    String secret,
    Duration readTimeout
  ) {
    HttpClient http = HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(3))
      .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(readTimeout);
    return RestClient
      .builder()
      .baseUrl(baseUrl)
      .requestFactory(factory)
      // 키는 헤더로만 보낸다. URL 에 넣으면 로그에 남을 수 있다.
      .defaultHeader("APCA-API-KEY-ID", keyId)
      .defaultHeader("APCA-API-SECRET-KEY", secret)
      .build();
  }

  // ---------- 시세 ----------

  @Override
  public boolean supportsBatch() {
    return true;
  }

  @Override
  public Optional<Quote> fetchQuote(String symbol) {
    return Optional.ofNullable(fetchQuotes(List.of(symbol)).get(symbol));
  }

  @Override
  public Map<String, Quote> fetchQuotes(Collection<String> symbols) {
    Map<String, Quote> result = new HashMap<>();
    List<String> all = new ArrayList<>(symbols);
    for (int i = 0; i < all.size(); i += BATCH_SIZE) {
      List<String> chunk = all.subList(i, Math.min(i + BATCH_SIZE, all.size()));
      Map<String, AlpacaSnapshot> body = call(() ->
        dataClient
          .get()
          .uri(uri ->
            uri
              .path("/v2/stocks/snapshots")
              .queryParam("symbols", String.join(",", chunk))
              .queryParam("feed", feed)
              .build()
          )
          .retrieve()
          .body(new ParameterizedTypeReference<Map<String, AlpacaSnapshot>>() {})
      );
      if (body == null) continue;
      Instant now = Instant.now();
      body.forEach((symbol, snap) ->
        toQuote(symbol, snap, now).ifPresent(q -> result.put(symbol, q))
      );
    }
    return result;
  }

  static Optional<Quote> toQuote(String symbol, AlpacaSnapshot snap, Instant now) {
    if (snap == null) return Optional.empty();

    Instant tradedAt = snap.latestTrade() == null
      ? null
      : parse(snap.latestTrade().t());
    BigDecimal price = snap.latestTrade() != null && snap.latestTrade().p() != null
      ? snap.latestTrade().p()
      : snap.dailyBar() != null ? snap.dailyBar().c() : null;
    if (price == null || price.signum() == 0) {
      return Optional.empty();
    }

    // 전일 종가. 오늘 봉이 이미 생겼으면 prevDailyBar 가, 아직 없으면 마지막 봉(dailyBar)이 전일 종가다.
    BigDecimal prevClose = null;
    AlpacaBar daily = snap.dailyBar();
    if (daily != null) {
      LocalDate barDate = dateInNewYork(parse(daily.t()));
      LocalDate tradeDate = tradedAt == null ? null : dateInNewYork(tradedAt);
      boolean sameDay = barDate != null && barDate.equals(tradeDate);
      if (sameDay && snap.prevDailyBar() != null) {
        prevClose = snap.prevDailyBar().c();
      } else if (!sameDay) {
        prevClose = daily.c();
      }
    }
    if (prevClose == null && snap.prevDailyBar() != null) {
      prevClose = snap.prevDailyBar().c();
    }

    BigDecimal change = null;
    BigDecimal changePercent = null;
    if (prevClose != null && prevClose.signum() != 0) {
      change = price.subtract(prevClose);
      changePercent =
        change
          .multiply(BigDecimal.valueOf(100))
          .divide(prevClose, 4, RoundingMode.HALF_UP);
    }

    return Optional.of(
      new Quote(
        symbol,
        price,
        change,
        changePercent,
        prevClose,
        daily == null ? null : daily.h(),
        daily == null ? null : daily.l(),
        daily == null ? null : daily.o(),
        now,
        tradedAt
      )
    );
  }

  private static Instant parse(String iso) {
    try {
      return iso == null ? null : Instant.parse(iso);
    } catch (Exception e) {
      return null;
    }
  }

  private static LocalDate dateInNewYork(Instant instant) {
    return instant == null ? null : instant.atZone(NEW_YORK).toLocalDate();
  }

  // ---------- 종목 검색 ----------

  @Override
  public List<SymbolInfo> search(String query) {
    String q = query.trim().toUpperCase(Locale.ROOT);
    if (q.isEmpty()) return List.of();

    List<Scored> hits = new ArrayList<>();
    for (SymbolInfo info : index().assets()) {
      int score = score(info, q);
      if (score > 0) hits.add(new Scored(info, score));
    }
    hits.sort(
      Comparator
        .comparingInt(Scored::score)
        .reversed()
        .thenComparing(s -> s.info().symbol())
    );
    return hits.stream().limit(MAX_RESULTS).map(Scored::info).toList();
  }

  /** 티커 일치 > 티커 접두 > 이름 접두 > 이름 포함 순으로 앞에 둔다. */
  private static int score(SymbolInfo info, String q) {
    String symbol = info.symbol();
    if (symbol.equals(q)) return 100;
    if (symbol.startsWith(q)) return 80;
    String name = info.name().toUpperCase(Locale.ROOT);
    if (name.startsWith(q)) return 60;
    if (name.contains(" " + q)) return 40;
    if (name.contains(q)) return 20;
    return 0;
  }

  @Override
  public Optional<SymbolInfo> fetchProfile(String symbol) {
    return Optional.ofNullable(index().bySymbol().get(symbol));
  }

  private AssetIndex index() {
    AssetIndex current = assetIndex;
    if (current != null && !current.isExpired()) {
      return current;
    }
    synchronized (this) {
      current = assetIndex;
      if (current != null && !current.isExpired()) {
        return current;
      }
      try {
        assetIndex = loadAssets();
      } catch (MarketDataException e) {
        // 갱신에 실패해도 예전 목록이 있으면 계속 쓴다.
        if (assetIndex == null) throw e;
        log.warn("종목 목록 갱신 실패, 이전 목록을 계속 사용합니다: {}", e.getMessage());
      }
      return assetIndex;
    }
  }

  private AssetIndex loadAssets() {
    List<AlpacaAsset> body = call(() ->
      tradingClient
        .get()
        .uri(uri ->
          uri
            .path("/v2/assets")
            .queryParam("status", "active")
            .queryParam("asset_class", "us_equity")
            .build()
        )
        .retrieve()
        .body(new ParameterizedTypeReference<List<AlpacaAsset>>() {})
    );
    if (body == null || body.isEmpty()) {
      throw new MarketDataException("Alpaca 종목 목록이 비어 있습니다", false, null);
    }

    List<SymbolInfo> assets = new ArrayList<>();
    Map<String, SymbolInfo> bySymbol = new HashMap<>();
    for (AlpacaAsset a : body) {
      if (a.symbol() == null || a.name() == null) continue;
      if (!Boolean.TRUE.equals(a.tradable())) continue;
      if (a.exchange() != null && EXCLUDED_EXCHANGES.contains(a.exchange())) continue;
      SymbolInfo info = new SymbolInfo(
        a.symbol().toUpperCase(Locale.ROOT),
        a.name(),
        "US"
      );
      assets.add(info);
      bySymbol.put(info.symbol(), info);
    }
    log.info("Alpaca 종목 목록 {}건 적재", assets.size());
    return new AssetIndex(List.copyOf(assets), Map.copyOf(bySymbol), Instant.now());
  }

  private <T> T call(java.util.function.Supplier<T> request) {
    try {
      return request.get();
    } catch (HttpClientErrorException.TooManyRequests e) {
      throw new MarketDataException("Alpaca 호출 한도 초과", true, e);
    } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.UnprocessableEntity e) {
      // 형식이 맞지 않는 심볼 등. 시세가 없는 것으로 취급한다.
      log.debug("Alpaca 요청 거부: {}", e.getMessage());
      return null;
    } catch (RestClientException e) {
      log.warn("Alpaca 호출 실패: {}", e.getMessage());
      throw new MarketDataException("Alpaca 호출 실패", false, e);
    }
  }

  private record Scored(SymbolInfo info, int score) {}

  private record AssetIndex(
    List<SymbolInfo> assets,
    Map<String, SymbolInfo> bySymbol,
    Instant loadedAt
  ) {
    boolean isExpired() {
      return Duration.between(loadedAt, Instant.now()).compareTo(ASSET_TTL) > 0;
    }
  }

  // ----- Alpaca 응답 형태. 시각은 문자열로 받아 직접 해석한다. -----

  public record AlpacaSnapshot(
    AlpacaTrade latestTrade,
    AlpacaBar dailyBar,
    AlpacaBar prevDailyBar
  ) {}

  public record AlpacaTrade(BigDecimal p, String t) {}

  public record AlpacaBar(
    String t,
    BigDecimal o,
    BigDecimal h,
    BigDecimal l,
    BigDecimal c
  ) {}

  public record AlpacaAsset(
    String symbol,
    String name,
    String exchange,
    Boolean tradable
  ) {}
}
