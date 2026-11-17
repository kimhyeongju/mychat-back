package com.khj.playground.common.exception;

import org.springframework.http.HttpStatus;

/** 모든 모듈이 공유하는 오류. 코드 대역은 9xxx. */
public enum CommonErrorCode implements ErrorCode {
  UNAUTHORIZED("unauthorized", HttpStatus.UNAUTHORIZED),
  FORBIDDEN("forbidden", HttpStatus.FORBIDDEN),
  VALIDATION_ERROR("validation-error", HttpStatus.BAD_REQUEST),
  MALFORMED_REQUEST("malformed-request", HttpStatus.BAD_REQUEST),
  RESOURCE_NOT_FOUND("resource-not-found", HttpStatus.NOT_FOUND),
  METHOD_NOT_ALLOWED("method-not-allowed", HttpStatus.METHOD_NOT_ALLOWED),
  PAYLOAD_TOO_LARGE("payload-too-large", HttpStatus.valueOf(413)),
  UNSUPPORTED_MEDIA_TYPE("unsupported-media-type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  TOO_MANY_REQUESTS("too-many-requests", HttpStatus.TOO_MANY_REQUESTS),
  UNKNOWN_ERROR("unknown-error", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String key;
  private final HttpStatus status;

  CommonErrorCode(String key, HttpStatus status) {
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
