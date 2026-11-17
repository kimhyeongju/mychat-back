package com.khj.playground.invest.exception;

import com.khj.playground.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** 모의투자 오류. 코드 대역 4xxx. 문구는 invest 모듈의 i18n/exception_*.yml. */
public enum InvestErrorCode implements ErrorCode {
  QUOTE_STALE_FOR_TRADE("quote-stale-for-trade", HttpStatus.SERVICE_UNAVAILABLE),
  SYMBOL_INVALID("symbol-invalid", HttpStatus.BAD_REQUEST),
  SYMBOL_NOT_FOUND("symbol-not-found", HttpStatus.NOT_FOUND),
  QUOTE_UNAVAILABLE("quote-unavailable", HttpStatus.SERVICE_UNAVAILABLE),
  SPLIT_IN_PROGRESS("split-in-progress", HttpStatus.SERVICE_UNAVAILABLE),
  SEARCH_QUERY_INVALID("search-query-invalid", HttpStatus.BAD_REQUEST),
  SEARCH_RATE_LIMITED("search-rate-limited", HttpStatus.TOO_MANY_REQUESTS),
  SEARCH_FAILED("search-failed", HttpStatus.SERVICE_UNAVAILABLE),
  SETTLEMENT_IN_PROGRESS("settlement-in-progress", HttpStatus.SERVICE_UNAVAILABLE),
  INSUFFICIENT_CASH("insufficient-cash", HttpStatus.BAD_REQUEST),
  INSUFFICIENT_HOLDINGS("insufficient-holdings", HttpStatus.BAD_REQUEST),
  WATCHLIST_FULL("watchlist-full", HttpStatus.BAD_REQUEST),
  PARTICIPANT_NOT_FOUND("participant-not-found", HttpStatus.NOT_FOUND),
  NEWS_UNAVAILABLE("news-unavailable", HttpStatus.SERVICE_UNAVAILABLE),
  POST_NOT_FOUND("post-not-found", HttpStatus.NOT_FOUND),
  COMMENT_NOT_FOUND("comment-not-found", HttpStatus.NOT_FOUND),
  POST_FORBIDDEN("post-forbidden", HttpStatus.FORBIDDEN),
  DISCUSSION_RATE_LIMITED("discussion-rate-limited", HttpStatus.TOO_MANY_REQUESTS);

  private final String key;
  private final HttpStatus status;

  InvestErrorCode(String key, HttpStatus status) {
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
