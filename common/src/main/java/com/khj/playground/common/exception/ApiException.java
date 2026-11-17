package com.khj.playground.common.exception;

import org.springframework.web.server.ResponseStatusException;

/**
 * 서비스 코드에서 던지는 오류. 사용자에게 보이는 문구는 여기에 적지 않고 ErrorCode 로만 가리킨다.
 * 문구는 요청의 Accept-Language 에 맞춰 GlobalExceptionHandler 가 yml 에서 찾아 채운다.
 *
 * ResponseStatusException 을 상속하므로 기존의 catch (ResponseStatusException) 코드가 그대로 동작한다.
 * reason 에는 오류 키가 들어간다(로그용).
 *
 * @param args 메시지의 {0}, {1} 자리에 들어갈 값. 숫자 서식이 로케일에 따라 바뀌지 않도록 문자열로 넘긴다.
 */
public class ApiException extends ResponseStatusException {

  private final transient ErrorCode errorCode;
  private final transient Object[] args;

  public ApiException(ErrorCode errorCode, Object... args) {
    super(errorCode.status(), errorCode.key());
    this.errorCode = errorCode;
    this.args = args == null ? new Object[0] : args;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public Object[] getArgs() {
    return args;
  }
}
