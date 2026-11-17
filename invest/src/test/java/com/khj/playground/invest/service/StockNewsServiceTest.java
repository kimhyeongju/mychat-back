package com.khj.playground.invest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StockNewsServiceTest {

  @Test
  void summaryDropsHtmlTagsAndCollapsesSpaces() {
    assertEquals(
      "Apple rises on guidance",
      StockNewsService.cleanSummary("<p>Apple  rises</p>\n<b>on</b> guidance")
    );
  }

  @Test
  void summaryIsNullWhenBlank() {
    assertNull(StockNewsService.cleanSummary(null));
    assertNull(StockNewsService.cleanSummary("<br/> "));
  }

  @Test
  void longSummaryIsTruncated() {
    String result = StockNewsService.cleanSummary("a".repeat(500));
    assertTrue(result.length() <= 301);
    assertTrue(result.endsWith("…"));
  }

  @Test
  void onlyHttpLinksAreAllowed() {
    assertEquals("https://example.com/a", StockNewsService.safeUrl("https://example.com/a"));
    assertEquals("http://example.com/a", StockNewsService.safeUrl(" http://example.com/a "));
    assertNull(StockNewsService.safeUrl("javascript:alert(1)"));
    assertNull(StockNewsService.safeUrl(null));
  }
}
