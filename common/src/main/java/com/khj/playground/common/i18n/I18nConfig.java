package com.khj.playground.common.i18n;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * messageSource 라는 이름의 빈은 스프링의 검증기(Bean Validation)도 함께 쓴다.
 * 그래서 DTO 의 message = "{user-dto.email.email}" 같은 키가 valid-message_*.yml 에서 풀린다.
 */
@Configuration
public class I18nConfig {

  @Bean(name = "messageSource")
  public YamlMessageSource messageSource() {
    return new YamlMessageSource();
  }

  /** Accept-Language 로 ko/en 중 고른다. 헤더가 없으면 한국어. */
  @Bean(name = "localeResolver")
  public LocaleResolver localeResolver() {
    return new AcceptHeaderLocaleResolver() {
      @Override
      public Locale resolveLocale(HttpServletRequest request) {
        return Locales.negotiate(request.getHeader("Accept-Language"));
      }
    };
  }
}
