package com.khj.playground.invest.market;

import java.util.List;
import java.util.Optional;

/**
 * 외부 시세 제공자 추상화.
 * 운영은 Finnhub, 키가 없는 환경은 서버가 만드는 가상 시세를 쓴다.
 * 호출 제한과 캐시는 이 계층이 아니라 PriceService 가 책임진다.
 */
public interface MarketDataClient {
  /**
   * @return 존재하지 않는 심볼이면 empty
   * @throws MarketDataException 제공자 오류, 호출 한도 초과 등
   */
  Optional<Quote> fetchQuote(String symbol);

  /** 미국 거래소 종목만 돌려준다. 실패하면 빈 목록 대신 예외를 던진다. */
  List<SymbolInfo> search(String query);

  /** @return 종목 이름을 찾지 못하면 empty */
  Optional<SymbolInfo> fetchProfile(String symbol);
}
