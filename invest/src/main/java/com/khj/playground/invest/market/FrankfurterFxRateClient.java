package com.khj.playground.invest.market;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 유럽중앙은행 고시 환율을 제공하는 Frankfurter. 키가 필요 없고 평일 하루 한 번 갱신된다.
 * Finnhub 무료 티어에는 환율이 없어서 별도로 둔다.
 */
@Slf4j
@Component
public class FrankfurterFxRateClient implements FxRateClient {

  private final RestClient client;

  public FrankfurterFxRateClient() {
    HttpClient http = HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(3))
      .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(5));
    this.client =
      RestClient
        .builder()
        .baseUrl("https://api.frankfurter.dev/v1")
        .requestFactory(factory)
        .build();
  }

  @Override
  public Optional<BigDecimal> fetchUsdKrw() {
    try {
      FrankfurterLatest body = client
        .get()
        .uri(uri ->
          uri
            .path("/latest")
            .queryParam("base", "USD")
            .queryParam("symbols", "KRW")
            .build()
        )
        .retrieve()
        .body(FrankfurterLatest.class);

      if (body == null || body.rates() == null) {
        return Optional.empty();
      }
      return Optional.ofNullable(body.rates().get("KRW"));
    } catch (RestClientException e) {
      log.warn("환율 조회 실패: {}", e.getMessage());
      return Optional.empty();
    }
  }

  public record FrankfurterLatest(
    String base,
    String date,
    Map<String, BigDecimal> rates
  ) {}
}
