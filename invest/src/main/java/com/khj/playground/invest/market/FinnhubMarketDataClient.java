package com.khj.playground.invest.market;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Finnhub 무료 티어.
 * - 분당 60회 제한이 있으므로 호출 횟수 조절은 PriceService 가 한다.
 * - 과거 봉(candle)은 유료라 쓰지 않는다. 시세는 /quote 하나로 받는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(
  name = "invest.market-data.provider",
  havingValue = "finnhub"
)
public class FinnhubMarketDataClient implements MarketDataClient {

  private static final String BASE_URL = "https://finnhub.io/api/v1";

  /** 검색 결과에서 허용하는 종목 유형. 선물·옵션·ADR 변형 등은 제외한다. */
  private static final Set<String> ALLOWED_TYPES = Set.of(
    "Common Stock",
    "ETP",
    "ETF",
    "REIT",
    "ADR"
  );

  private final RestClient client;

  public FinnhubMarketDataClient(
    @Value("${invest.market-data.finnhub.api-key}") String apiKey
  ) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
        "invest.market-data.provider=finnhub 이면 FINNHUB_API_KEY 가 필요합니다."
      );
    }
    HttpClient http = HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(3))
      .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(5));

    this.client =
      RestClient
        .builder()
        .baseUrl(BASE_URL)
        .requestFactory(factory)
        // 키는 쿼리스트링이 아니라 헤더로 보내 로그에 남지 않게 한다.
        .defaultHeader("X-Finnhub-Token", apiKey)
        .build();
  }

  @Override
  public Optional<Quote> fetchQuote(String symbol) {
    FinnhubQuote body = call(() ->
      client
        .get()
        .uri(uri -> uri.path("/quote").queryParam("symbol", symbol).build())
        .retrieve()
        .body(FinnhubQuote.class)
    );

    // 없는 심볼이면 모든 값이 0 으로 온다.
    if (body == null || body.c() == null || body.c().signum() == 0) {
      return Optional.empty();
    }

    return Optional.of(
      new Quote(
        symbol,
        body.c(),
        body.d(),
        body.dp(),
        body.pc(),
        body.h(),
        body.l(),
        body.o(),
        Instant.now()
      )
    );
  }

  @Override
  public List<SymbolInfo> search(String query) {
    FinnhubSearch body = call(() ->
      client
        .get()
        .uri(uri ->
          uri
            .path("/search")
            .queryParam("q", query)
            .queryParam("exchange", "US")
            .build()
        )
        .retrieve()
        .body(FinnhubSearch.class)
    );

    if (body == null || body.result() == null) {
      return List.of();
    }

    return body
      .result()
      .stream()
      .filter(r -> r.symbol() != null && r.description() != null)
      .filter(r -> r.type() == null || ALLOWED_TYPES.contains(r.type()))
      // "BRK.B" 같은 클래스 주식은 두고, "AAPL:US" 처럼 거래소가 붙은 변형은 뺀다.
      .filter(r -> !r.symbol().contains(":"))
      .map(r ->
        new SymbolInfo(
          r.symbol().toUpperCase(Locale.ROOT),
          r.description(),
          "US"
        )
      )
      .limit(20)
      .toList();
  }

  @Override
  public Optional<SymbolInfo> fetchProfile(String symbol) {
    FinnhubProfile body = call(() ->
      client
        .get()
        .uri(uri ->
          uri.path("/stock/profile2").queryParam("symbol", symbol).build()
        )
        .retrieve()
        .body(FinnhubProfile.class)
    );

    if (body == null || body.name() == null || body.name().isBlank()) {
      return Optional.empty();
    }
    return Optional.of(new SymbolInfo(symbol, body.name(), body.exchange()));
  }

  private <T> T call(java.util.function.Supplier<T> request) {
    try {
      return request.get();
    } catch (HttpClientErrorException.TooManyRequests e) {
      throw new MarketDataException("Finnhub 호출 한도 초과", true, e);
    } catch (RestClientException e) {
      log.warn("Finnhub 호출 실패: {}", e.getMessage());
      throw new MarketDataException("Finnhub 호출 실패", false, e);
    }
  }

  // ----- Finnhub 응답 형태. 필드명은 API 그대로 둔다. -----

  public record FinnhubQuote(
    BigDecimal c,
    BigDecimal d,
    BigDecimal dp,
    BigDecimal h,
    BigDecimal l,
    BigDecimal o,
    BigDecimal pc,
    Long t
  ) {}

  public record FinnhubSearch(Integer count, List<FinnhubSearchItem> result) {}

  public record FinnhubSearchItem(
    String description,
    String displaySymbol,
    String symbol,
    String type
  ) {}

  public record FinnhubProfile(String name, String exchange, String ticker) {}
}
