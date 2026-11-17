package com.khj.playground.common.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class LocalesTest {

  @Test
  void missingHeaderIsKorean() {
    assertEquals(Locale.KOREAN, Locales.negotiate(null));
    assertEquals(Locale.KOREAN, Locales.negotiate(""));
  }

  @Test
  void picksEnglishWhenPreferred() {
    assertEquals(Locale.ENGLISH, Locales.negotiate("en-US,en;q=0.9,ko;q=0.8"));
    assertEquals(Locale.ENGLISH, Locales.negotiate("en"));
  }

  @Test
  void picksKoreanWhenPreferred() {
    assertEquals(Locale.KOREAN, Locales.negotiate("ko-KR,ko;q=0.9,en-US;q=0.8"));
  }

  @Test
  void unsupportedOrBrokenFallsBackToKorean() {
    assertEquals(Locale.KOREAN, Locales.negotiate("ja-JP,ja;q=0.9"));
    assertEquals(Locale.KOREAN, Locales.negotiate("!!not a header!!"));
  }
}
