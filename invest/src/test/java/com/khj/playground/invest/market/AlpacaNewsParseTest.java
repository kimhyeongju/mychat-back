package com.khj.playground.invest.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AlpacaNewsParseTest {

  @Test
  void parsesArticles() {
    Map<String, Object> body = Map.of(
      "news",
      List.of(
        Map.of(
          "id",
          24803233,
          "headline",
          "Apple beats estimates",
          "summary",
          "Strong quarter",
          "source",
          "benzinga",
          "url",
          "https://www.benzinga.com/x",
          "created_at",
          "2024-03-22T14:44:27Z"
        ),
        Map.of("id", 2, "headline", "No extras")
      )
    );
    List<NewsItem> items = AlpacaMarketDataClient.parseNews(body);
    assertEquals(2, items.size());
    assertEquals(24803233L, items.get(0).id());
    assertEquals(Instant.parse("2024-03-22T14:44:27Z"), items.get(0).createdAt());
    assertNull(items.get(1).summary());
    assertNull(items.get(1).createdAt());
  }

  @Test
  void skipsMalformedEntries() {
    Map<String, Object> body = Map.of(
      "news",
      List.of(Map.of("headline", "no id"), "not a map", Map.of("id", 3))
    );
    assertEquals(0, AlpacaMarketDataClient.parseNews(body).size());
    assertEquals(0, AlpacaMarketDataClient.parseNews(Map.of()).size());
  }
}
