package com.khj.playground.invest.service;

import com.khj.playground.common.event.UserWithdrawnEvent;
import com.khj.playground.invest.repository.InvestAccountRepository;
import com.khj.playground.invest.repository.WatchlistRepository;
import com.khj.playground.invest.repository.WeeklyResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원이 탈퇴(또는 운영자가 삭제)하면 모의투자 데이터를 모두 지운다.
 * 이 리스너는 탈퇴 트랜잭션 안에서 동기로 실행되므로, 여기서 실패하면 탈퇴도 취소된다.
 *
 * - 계좌를 지우면 DB 의 ON DELETE CASCADE 로 보유 종목과 거래 내역이 함께 지워진다.
 * - 관심 종목과 주간 성적(포인트)도 지운다. 이미 정산된 주의 다른 사람 순위는 바뀌지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvestUserCleanup {

  private final InvestAccountRepository accountRepository;
  private final WatchlistRepository watchlistRepository;
  private final WeeklyResultRepository resultRepository;
  private final RankingService rankingService;
  private final ScoreService scoreService;

  @EventListener
  public void onUserWithdrawn(UserWithdrawnEvent event) {
    accountRepository.findByUserId(event.userId()).ifPresent(accountRepository::delete);
    int watched = watchlistRepository.deleteAllByUserId(event.userId());
    int results = resultRepository.deleteAllByUserId(event.userId());
    rankingService.invalidate();
    scoreService.invalidate();
    log.info(
      "탈퇴 회원의 모의투자 데이터 삭제 userId={} 관심종목={} 주간성적={}",
      event.userId(),
      watched,
      results
    );
  }
}
