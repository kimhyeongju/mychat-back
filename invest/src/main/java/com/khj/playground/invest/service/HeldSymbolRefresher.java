package com.khj.playground.invest.service;

import com.khj.playground.invest.repository.HoldingRepository;
import com.khj.playground.invest.repository.WatchlistRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 누군가 보유 중이거나 관심 종목으로 둔 종목의 시세를 주기적으로 갱신한다.
 * 포트폴리오 평가와 랭킹이 아무도 안 보는 사이에도 너무 오래되지 않게 하기 위함이다.
 * - 일괄 조회가 되는 제공자(Alpaca): 모든 보유 종목을 한 번에 갱신한다. 호출은 50종목당 1회.
 * - 그렇지 않은 제공자(Finnhub): 종목마다 호출하므로 한 번에 최대 40종목만 처리해
 *   분당 호출 한도의 나머지를 사용자 요청 몫으로 남긴다.
 * 실행 주기는 invest.market-data.refresh-interval-ms 로 정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeldSymbolRefresher {

  private static final int MAX_PER_RUN = 40;

  private final HoldingRepository holdingRepository;
  private final WatchlistRepository watchlistRepository;
  private final PriceService priceService;

  @Scheduled(
    fixedDelayString = "${invest.market-data.refresh-interval-ms:60000}",
    initialDelay = 15_000
  )
  public void refresh() {
    List<String> symbols;
    try {
      // 보유 종목 + 관심 종목. 순위·포트폴리오 평가와 관심 종목 목록이 최신 시세를 보게 한다.
      Set<String> all = new LinkedHashSet<>(holdingRepository.findDistinctSymbols());
      all.addAll(watchlistRepository.findDistinctSymbols());
      symbols = List.copyOf(all);
    } catch (Exception e) {
      log.warn("보유 종목 목록 조회 실패: {}", e.getMessage());
      return;
    }

    if (priceService.supportsBatch()) {
      int count = priceService.refreshBatch(symbols);
      if (count > 0) {
        log.debug("보유 종목 시세 일괄 갱신 {}건", count);
      }
      return;
    }

    int refreshed = 0;
    for (String symbol : symbols) {
      if (refreshed >= MAX_PER_RUN) break;
      try {
        if (priceService.refreshIfStale(symbol)) {
          refreshed++;
        }
      } catch (Exception e) {
        log.debug("보유 종목 시세 갱신 실패 symbol={}: {}", symbol, e.getMessage());
      }
    }
    if (refreshed > 0) {
      log.debug("보유 종목 시세 갱신 {}건", refreshed);
    }
  }
}
