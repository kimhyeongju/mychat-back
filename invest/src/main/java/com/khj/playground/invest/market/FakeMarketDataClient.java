package com.khj.playground.invest.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬·dev 용 가상 시세. API 키 없이 동작한다.
 * 심볼마다 고정된 시작가에서 호출할 때마다 ±0.5% 안에서 무작위로 움직인다.
 * 서버를 재시작하면 시작가로 돌아온다.
 */
@Component
@ConditionalOnProperty(
  name = "invest.market-data.provider",
  havingValue = "fake",
  matchIfMissing = true
)
public class FakeMarketDataClient implements MarketDataClient {

  private static final List<SymbolInfo> CATALOG = List.of(
    new SymbolInfo("AAPL", "Apple Inc", "US"),
    new SymbolInfo("MSFT", "Microsoft Corp", "US"),
    new SymbolInfo("NVDA", "NVIDIA Corp", "US"),
    new SymbolInfo("AMZN", "Amazon.com Inc", "US"),
    new SymbolInfo("GOOGL", "Alphabet Inc Class A", "US"),
    new SymbolInfo("META", "Meta Platforms Inc", "US"),
    new SymbolInfo("TSLA", "Tesla Inc", "US"),
    new SymbolInfo("NFLX", "Netflix Inc", "US"),
    new SymbolInfo("AMD", "Advanced Micro Devices Inc", "US"),
    new SymbolInfo("INTC", "Intel Corp", "US"),
    new SymbolInfo("KO", "Coca-Cola Co", "US"),
    new SymbolInfo("DIS", "Walt Disney Co", "US"),
    new SymbolInfo("SPY", "SPDR S&P 500 ETF Trust", "US"),
    new SymbolInfo("QQQ", "Invesco QQQ Trust", "US")
  );

  private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

  @Override
  public Optional<Quote> fetchQuote(String symbol) {
    if (!isKnown(symbol)) {
      return Optional.empty();
    }

    BigDecimal previous = lastPrices.computeIfAbsent(symbol, this::basePrice);
    double drift = ThreadLocalRandom.current().nextDouble(-0.005, 0.005);
    BigDecimal price = previous
      .multiply(BigDecimal.valueOf(1 + drift))
      .setScale(2, RoundingMode.HALF_UP);
    lastPrices.put(symbol, price);

    BigDecimal base = basePrice(symbol);
    BigDecimal change = price.subtract(base);
    BigDecimal changePercent = change
      .multiply(BigDecimal.valueOf(100))
      .divide(base, 2, RoundingMode.HALF_UP);

    return Optional.of(
      new Quote(
        symbol,
        price,
        change,
        changePercent,
        base,
        price.max(base),
        price.min(base),
        base,
        Instant.now()
      )
    );
  }

  @Override
  public List<SymbolInfo> search(String query) {
    String q = query.toUpperCase(Locale.ROOT);
    return CATALOG
      .stream()
      .filter(s ->
        s.symbol().contains(q) ||
        s.name().toUpperCase(Locale.ROOT).contains(q)
      )
      .toList();
  }

  @Override
  public Optional<SymbolInfo> fetchProfile(String symbol) {
    return CATALOG.stream().filter(s -> s.symbol().equals(symbol)).findFirst();
  }

  /** 카탈로그에 있거나, 알파벳 1~5자 심볼이면 존재하는 것으로 본다. */
  private boolean isKnown(String symbol) {
    return (
      CATALOG.stream().anyMatch(s -> s.symbol().equals(symbol)) ||
      symbol.matches("^[A-Z]{1,5}$")
    );
  }

  /** 심볼 해시로 20~520 달러 사이의 시작가를 정한다. 같은 심볼은 항상 같은 시작가다. */
  private BigDecimal basePrice(String symbol) {
    int bucket = Math.floorMod(symbol.hashCode(), 500);
    return BigDecimal.valueOf(20 + bucket).setScale(2, RoundingMode.HALF_UP);
  }
}
