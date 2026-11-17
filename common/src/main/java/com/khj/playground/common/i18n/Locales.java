package com.khj.playground.common.i18n;

import java.util.List;
import java.util.Locale;

/** 지원 언어(ko, en)와 Accept-Language 협상. 헤더가 없거나 지원하지 않는 언어면 한국어다. */
public final class Locales {

  public static final Locale DEFAULT = Locale.KOREAN;
  private static final List<Locale> SUPPORTED = List.of(Locale.KOREAN, Locale.ENGLISH);

  private Locales() {}

  public static Locale negotiate(String acceptLanguage) {
    if (acceptLanguage == null || acceptLanguage.isBlank()) {
      return DEFAULT;
    }
    try {
      Locale match = Locale.lookup(Locale.LanguageRange.parse(acceptLanguage), SUPPORTED);
      return match != null ? match : DEFAULT;
    } catch (IllegalArgumentException e) {
      return DEFAULT;
    }
  }
}
