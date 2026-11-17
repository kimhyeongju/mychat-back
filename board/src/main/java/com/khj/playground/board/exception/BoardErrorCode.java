package com.khj.playground.board.exception;

import com.khj.playground.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** 게시판 오류. 코드 대역 3xxx. 문구는 board 모듈의 i18n/exception_*.yml. */
public enum BoardErrorCode implements ErrorCode {
  ATTACHMENT_EMPTY("attachment-empty", HttpStatus.BAD_REQUEST),
  ATTACHMENT_LIMIT("attachment-limit", HttpStatus.BAD_REQUEST),
  POST_NOT_FOUND("post-not-found", HttpStatus.NOT_FOUND),
  ATTACHMENT_NOT_FOUND("attachment-not-found", HttpStatus.NOT_FOUND),
  BOARD_NOT_FOUND("board-not-found", HttpStatus.NOT_FOUND),
  COMMENT_NOT_FOUND("comment-not-found", HttpStatus.NOT_FOUND),
  FILE_NAME_INVALID("file-name-invalid", HttpStatus.BAD_REQUEST),
  FILE_SAVE_FAILED("file-save-failed", HttpStatus.INTERNAL_SERVER_ERROR),
  FILE_NOT_FOUND("file-not-found", HttpStatus.NOT_FOUND),
  FILE_READ_FAILED("file-read-failed", HttpStatus.INTERNAL_SERVER_ERROR),
  FILE_EXTENSION_MISSING("file-extension-missing", HttpStatus.BAD_REQUEST),
  FILE_TYPE_NOT_ALLOWED("file-type-not-allowed", HttpStatus.BAD_REQUEST),
  BOARD_ADMIN_ONLY("board-admin-only", HttpStatus.FORBIDDEN);

  private final String key;
  private final HttpStatus status;

  BoardErrorCode(String key, HttpStatus status) {
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
