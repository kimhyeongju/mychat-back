package com.khj.playground.common.i18n;

import com.khj.playground.common.exception.ErrorCode;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** ErrorCode 의 코드 번호와 문구를 언어에 맞춰 찾는다. */
@Component
public class ErrorMessages {

  private final YamlMessageSource messageSource;

  public ErrorMessages(YamlMessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public String code(ErrorCode errorCode) {
    return messageSource.getMessage(errorCode.key() + ".code", null, "0000", Locale.KOREAN);
  }

  public String message(ErrorCode errorCode, Locale locale, Object... args) {
    return messageSource.getMessage(
      errorCode.key() + ".message",
      args == null || args.length == 0 ? null : args,
      errorCode.key(),
      locale
    );
  }
}
