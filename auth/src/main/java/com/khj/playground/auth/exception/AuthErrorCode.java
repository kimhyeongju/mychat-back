package com.khj.playground.auth.exception;

import com.khj.playground.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** 인증/회원 오류. 코드 대역 1xxx. 문구는 auth 모듈의 i18n/exception_*.yml. */
public enum AuthErrorCode implements ErrorCode {
  PHONE_VERIFICATION_REQUIRED("phone-verification-required", HttpStatus.BAD_REQUEST),
  USERNAME_DUPLICATED("username-duplicated", HttpStatus.CONFLICT),
  NICKNAME_DUPLICATED("nickname-duplicated", HttpStatus.CONFLICT),
  PHONE_DUPLICATED("phone-duplicated", HttpStatus.CONFLICT),
  LOGIN_FAILED("login-failed", HttpStatus.UNAUTHORIZED),
  TOKEN_INVALID("token-invalid", HttpStatus.UNAUTHORIZED),
  TOKEN_REVOKED("token-revoked", HttpStatus.UNAUTHORIZED),
  USER_SESSION_INVALID("user-session-invalid", HttpStatus.UNAUTHORIZED),
  USER_NOT_FOUND("user-not-found", HttpStatus.NOT_FOUND),
  ACCOUNT_NOT_FOUND_BY_PHONE("account-not-found-by-phone", HttpStatus.NOT_FOUND),
  USERNAME_NOT_FOUND("username-not-found", HttpStatus.NOT_FOUND),
  FIND_ACCOUNT_MISMATCH("find-account-mismatch", HttpStatus.BAD_REQUEST),
  PASSWORD_INCORRECT("password-incorrect", HttpStatus.BAD_REQUEST),
  SMS_RATE_LIMIT_MINUTE("sms-rate-limit-minute", HttpStatus.TOO_MANY_REQUESTS),
  SMS_RATE_LIMIT_DAILY("sms-rate-limit-daily", HttpStatus.TOO_MANY_REQUESTS),
  SMS_SEND_FAILED("sms-send-failed", HttpStatus.SERVICE_UNAVAILABLE),
  VERIFICATION_CODE_INVALID("verification-code-invalid", HttpStatus.BAD_REQUEST),
  ADMIN_CANNOT_MODIFY("admin-cannot-modify", HttpStatus.FORBIDDEN),
  ADMIN_CANNOT_DELETE("admin-cannot-delete", HttpStatus.FORBIDDEN),
  PHONE_GENERATION_FAILED("phone-generation-failed", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String key;
  private final HttpStatus status;

  AuthErrorCode(String key, HttpStatus status) {
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
