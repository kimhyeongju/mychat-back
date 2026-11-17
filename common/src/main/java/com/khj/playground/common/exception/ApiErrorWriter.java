package com.khj.playground.common.exception;

import com.khj.playground.common.i18n.ErrorMessages;
import com.khj.playground.common.i18n.Locales;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 컨트롤러에 닿기 전(보안 필터)에 나는 오류를 GlobalExceptionHandler 와 같은 JSON 으로 쓴다.
 * 이 단계에서는 스프링 MVC 가 로케일을 정해 주지 않으므로 Accept-Language 를 직접 해석한다.
 */
@Component
public class ApiErrorWriter {

  private final ErrorMessages messages;

  public ApiErrorWriter(ErrorMessages messages) {
    this.messages = messages;
  }

  public void write(
    HttpServletRequest request,
    HttpServletResponse response,
    ErrorCode code,
    Object... args
  ) throws IOException {
    if (response.isCommitted()) {
      return;
    }
    Locale locale = Locales.negotiate(request.getHeader("Accept-Language"));
    String json =
      "{\"code\":" + quote(messages.code(code)) +
      ",\"message\":" + quote(messages.message(code, locale, args)) +
      ",\"status\":" + code.status().value() +
      ",\"fieldErrors\":[]}";

    response.setStatus(code.status().value());
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(json);
  }

  private static String quote(String value) {
    StringBuilder sb = new StringBuilder("\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.append('"').toString();
  }
}
