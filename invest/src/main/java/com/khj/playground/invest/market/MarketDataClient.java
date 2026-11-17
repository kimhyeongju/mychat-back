package com.khj.playground.invest.market;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 외부 시세 제공자 추상화.
 * 운영은 Alpaca(또는 Finnhub), 키가 없는 환경은 서버가 만드는 가상 시세를 쓴다.
 * 호출 제한과 캐시는 이 계층이 아니라 PriceService 가 책임진다.
 */
public interface MarketDataClient {
  /**
   * @return 존재하지 않는 심볼이면 empty
   * @throws MarketDataException 제공자 오류, 호출 한도 초과 등
   */
  Optional<Quote> fetchQuote(String symbol);

  /**
   * 여러 종목을 한 번에 받는다. 존재하지 않는 심볼은 결과에서 빠진다.
   * 일괄 조회를 지원하지 않는 제공자는 기본 구현(종목별 호출)을 쓴다.
   */
  default Map<String, Quote> fetchQuotes(Collection<String> symbols) {
    Map<String, Quote> result = new HashMap<>();
    for (String symbol : symbols) {
      fetchQuote(symbol).ifPresent(q -> result.put(symbol, q));
    }
    return result;
  }

  /** true 이면 fetchQuotes 가 호출 한 번으로 여러 종목을 처리한다. */
  default boolean supportsBatch() {
    return false;
  }

  /**
   * 분할/병합 일정. ex-date 가 [from, to] 인 것만 준다. 지원하지 않는 제공자는 빈 목록이다.
   *
   * @throws MarketDataException 제공자 오류
   */
  default List<StockSplitInfo> fetchSplits(
    Collection<String> symbols,
    java.time.LocalDate from,
    java.time.LocalDate to
  ) {
    return List.of();
  }

  /** 미국 거래소 종목만 돌려준다. 실패하면 빈 목록 대신 예외를 던진다. */
  List<SymbolInfo> search(String query);

  /** @return 종목 이름을 찾지 못하면 empty */
  Optional<SymbolInfo> fetchProfile(String symbol);
}
