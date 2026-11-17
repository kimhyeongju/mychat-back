package com.khj.playground.common.exception;

import com.khj.playground.common.i18n.ErrorMessages;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 모든 컨트롤러의 오류를 { code, message, status, fieldErrors } 로 통일한다.
 * 문구는 요청 언어(Accept-Language)에 맞춰 yml 에서 찾는다.
 * 보안 필터 단계(로그인 안 됨, 권한 없음)는 컨트롤러 밖이라 ApiErrorWriter 가 같은 형식으로 쓴다.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final ErrorMessages messages;

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorBody> handleApi(ApiException e) {
    return build(e.getErrorCode().status(), e.getErrorCode(), e.getArgs());
  }

  /** ApiException 으로 바꾸지 않은 예전 코드. 적어 둔 문구가 있으면 그대로 보여준다. */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorBody> handleStatus(ResponseStatusException e) {
    HttpStatus status = resolve(e.getStatusCode().value());
    ErrorCode code = forStatus(status);
    String reason = e.getReason();
    if (reason == null || reason.isBlank()) {
      return build(status, code);
    }
    return ResponseEntity
      .status(status)
      .body(new ErrorBody(messages.code(code), reason, status.value(), List.of()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorBody> handleInvalid(MethodArgumentNotValidException e) {
    List<ErrorBody.FieldIssue> issues = e
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .map(f -> new ErrorBody.FieldIssue(f.getField(), f.getDefaultMessage()))
      .toList();
    return validation(issues);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorBody> handleViolation(ConstraintViolationException e) {
    List<ErrorBody.FieldIssue> issues = e
      .getConstraintViolations()
      .stream()
      .map((ConstraintViolation<?> v) ->
        new ErrorBody.FieldIssue(String.valueOf(v.getPropertyPath()), v.getMessage())
      )
      .toList();
    return validation(issues);
  }

  @ExceptionHandler(
    {
      HttpMessageNotReadableException.class,
      ServletRequestBindingException.class,
      TypeMismatchException.class,
    }
  )
  public ResponseEntity<ErrorBody> handleMalformed(Exception e) {
    log.debug("malformed request: {}", e.getMessage());
    return build(HttpStatus.BAD_REQUEST, CommonErrorCode.MALFORMED_REQUEST);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorBody> handleTooLarge(MaxUploadSizeExceededException e) {
    return build(CommonErrorCode.PAYLOAD_TOO_LARGE.status(), CommonErrorCode.PAYLOAD_TOO_LARGE);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorBody> handleAuthentication(AuthenticationException e) {
    return build(HttpStatus.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorBody> handleDenied(AccessDeniedException e) {
    return build(HttpStatus.FORBIDDEN, CommonErrorCode.FORBIDDEN);
  }

  /** 위에서 다루지 않은 것. 스프링 MVC 가 상태 코드를 가진 오류(404, 405, 415 등)는 그 상태를 유지한다. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorBody> handleAny(Exception e) {
    if (e instanceof ErrorResponse er) {
      HttpStatus status = resolve(er.getStatusCode().value());
      if (status.is5xxServerError()) {
        log.error("unhandled error response", e);
      }
      return build(status, forStatus(status));
    }
    log.error("unhandled exception", e);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.UNKNOWN_ERROR);
  }

  // ----- helpers -----

  private ResponseEntity<ErrorBody> validation(List<ErrorBody.FieldIssue> issues) {
    Locale locale = LocaleContextHolder.getLocale();
    ErrorCode code = CommonErrorCode.VALIDATION_ERROR;
    // 화면에는 첫 번째 사유를 바로 보여줄 수 있게 message 에 넣는다.
    String message = issues.isEmpty() || issues.get(0).message() == null
      ? messages.message(code, locale)
      : issues.get(0).message();
    return ResponseEntity
      .status(code.status())
      .body(new ErrorBody(messages.code(code), message, code.status().value(), issues));
  }

  private ResponseEntity<ErrorBody> build(HttpStatus status, ErrorCode code, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return ResponseEntity
      .status(status)
      .body(
        new ErrorBody(
          messages.code(code),
          messages.message(code, locale, args),
          status.value(),
          List.of()
        )
      );
  }

  private static HttpStatus resolve(int value) {
    HttpStatus status = HttpStatus.resolve(value);
    return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
  }

  static ErrorCode forStatus(HttpStatus status) {
    return switch (status.value()) {
      case 400 -> CommonErrorCode.MALFORMED_REQUEST;
      case 401 -> CommonErrorCode.UNAUTHORIZED;
      case 403 -> CommonErrorCode.FORBIDDEN;
      case 404 -> CommonErrorCode.RESOURCE_NOT_FOUND;
      case 405 -> CommonErrorCode.METHOD_NOT_ALLOWED;
      case 413 -> CommonErrorCode.PAYLOAD_TOO_LARGE;
      case 415 -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
      case 429 -> CommonErrorCode.TOO_MANY_REQUESTS;
      default -> status.is5xxServerError()
        ? CommonErrorCode.UNKNOWN_ERROR
        : CommonErrorCode.MALFORMED_REQUEST;
    };
  }
}
