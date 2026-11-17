package com.khj.playground.invest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 분할/병합 일정을 주기적으로 받아 오고(기본 1시간), 적용할 때가 된 것은 30초마다 확인한다.
 * 스케줄러 스레드가 하나라서 주간 정산과 겹쳐 실행되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockSplitScheduler {

  private final StockSplitService splitService;

  @Value("${invest.splits.enabled:true}")
  private boolean enabled;

  @Scheduled(
    fixedDelayString = "${invest.splits.sync-interval-ms:3600000}",
    initialDelay = 60_000
  )
  public void sync() {
    if (!enabled) return;
    try {
      splitService.sync();
    } catch (Exception e) {
      log.warn("분할/병합 일정 동기화 실패: {}", e.getMessage());
    }
  }

  @Scheduled(fixedDelay = 30_000, initialDelay = 75_000)
  public void apply() {
    if (!enabled) return;
    try {
      splitService.applyDue();
    } catch (Exception e) {
      log.error("분할/병합 적용 중 오류", e);
    }
  }
}
