package com.khj.playground.invest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.khj.playground.common.i18n.ErrorMessages;
import com.khj.playground.common.i18n.YamlMessageSource;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class InvestMessagesTest {

  private final ErrorMessages messages = new ErrorMessages(new YamlMessageSource());

  @Test
  void argumentsAreFilledIn() {
    assertEquals(
      "현금이 부족합니다. 필요 금액 $1000.00",
      messages.message(InvestErrorCode.INSUFFICIENT_CASH, Locale.KOREAN, "1000.00")
    );
    assertEquals(
      "Not enough cash. Required amount: $1000.00",
      messages.message(InvestErrorCode.INSUFFICIENT_CASH, Locale.ENGLISH, "1000.00")
    );
  }

  @Test
  void codeComesFromYaml() {
    assertEquals("4010", messages.code(InvestErrorCode.INSUFFICIENT_CASH));
  }
}
