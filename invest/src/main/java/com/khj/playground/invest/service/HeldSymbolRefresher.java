package com.khj.playground.invest.service;

import com.khj.playground.invest.repository.HoldingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 누군가 보유 중인 종목의 시세를 주기적으로 갱신한다.
 * 포트폴리오 평가와 랭킹이 아무도 안 보는 사이에도 너무 오래되지 않게 하기 위함이다.
 * 한 번에 최대 40종목만 처리해 분당 호출 한도의 나머지를 사용자 요청 몫으로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeldSymbolRefresher {

  private static final int MAX_PER_RUN = 40;

  private final HoldingRepository holdingRepository;
  private final PriceService priceService;

  @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
  public void refresh() {
    List<String> symbols;
    try {
      symbols = holdingRepository.findDistinctSymbols();
    } catch (Exception e) {
      log.warn("보유 종목 목록 조회 실패: {}", e.getMessage());
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
