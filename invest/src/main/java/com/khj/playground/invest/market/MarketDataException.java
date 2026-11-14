package com.khj.playground.invest.market;

/** 시세 제공자 호출 실패. 호출 한도 초과 여부를 구분해 폴백 정책을 다르게 가져간다. */
public class MarketDataException extends RuntimeException {

  private final boolean rateLimited;

  public MarketDataException(String message, boolean rateLimited, Throwable cause) {
    super(message, cause);
    this.rateLimited = rateLimited;
  }

  public boolean isRateLimited() {
    return rateLimited;
  }
}
