package com.khj.playground.chat.exception;

import com.khj.playground.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** 채팅 오류. 코드 대역 2xxx. 문구는 chat 모듈의 i18n/exception_*.yml. */
public enum ChatErrorCode implements ErrorCode {
  MESSAGE_NOT_FOUND("message-not-found", HttpStatus.NOT_FOUND),
  MESSAGE_RATE_LIMITED("message-rate-limited", HttpStatus.TOO_MANY_REQUESTS),
  ROOM_FORBIDDEN("room-forbidden", HttpStatus.FORBIDDEN),
  ROOM_NOT_FOUND("room-not-found", HttpStatus.NOT_FOUND),
  ANONYMOUS_SESSION_REQUIRED("anonymous-session-required", HttpStatus.UNAUTHORIZED),
  ANONYMOUS_SESSION_INVALID("anonymous-session-invalid", HttpStatus.UNAUTHORIZED),
  DIRECT_CHAT_SELF("direct-chat-self", HttpStatus.BAD_REQUEST),
  DIRECT_PEER_NOT_FOUND("direct-peer-not-found", HttpStatus.NOT_FOUND),
  DIRECT_PARTICIPANT_MISSING("direct-participant-missing", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String key;
  private final HttpStatus status;

  ChatErrorCode(String key, HttpStatus status) {
    this.key = key;
    this.status = status;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public HttpStatus status() {
    return status;
  }
}
