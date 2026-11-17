package com.khj.playground.common.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class YamlMessageSourceTest {

  private final YamlMessageSource source = new YamlMessageSource();

  @Test
  void resolvesKoreanAndEnglish() {
    String ko = source.getMessage("unauthorized.message", null, Locale.KOREAN);
    String en = source.getMessage("unauthorized.message", null, Locale.ENGLISH);
    assertTrue(ko.contains("로그인"));
    assertTrue(en.contains("Login"));
  }

  @Test
  void codeIsTheSameForEveryLanguage() {
    assertEquals(
      source.getMessage("forbidden.code", null, Locale.KOREAN),
      source.getMessage("forbidden.code", null, Locale.ENGLISH)
    );
  }

  @Test
  void unsupportedLanguageFallsBackToKorean() {
    assertEquals(
      source.getMessage("forbidden.message", null, Locale.KOREAN),
      source.getMessage("forbidden.message", null, Locale.JAPANESE)
    );
  }

  @Test
  void unknownKeyReturnsDefault() {
    assertNull(source.getMessage("no.such.key", null, null, Locale.KOREAN));
  }
}
