package com.khj.playground.invest.service;

import com.khj.playground.invest.entity.InvestWeek;
import com.khj.playground.invest.entity.WeeklyResult;
import com.khj.playground.invest.repository.HoldingRepository;
import com.khj.playground.invest.repository.InvestAccountRepository;
import com.khj.playground.invest.repository.InvestWeekRepository;
import com.khj.playground.invest.repository.TradeRepository;
import com.khj.playground.invest.repository.WeeklyResultRepository;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한 주를 마감한다. 한 트랜잭션 안에서
 * 1) 그 주에 거래한 사용자의 마감 시점 수익률로 순위·포인트를 기록하고
 * 2) 모든 계좌를 시작 자금으로 되돌리고 보유 종목을 지운다.
 * 도중에 실패하면 전부 되돌아가므로 "포인트만 주고 초기화는 안 된" 상태가 생기지 않는다.
 * week_end_date 유니크 제약이 같은 주의 중복 정산을 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklySettlementService {

  private final AccountValuation valuation;
  private final TradeRepository tradeRepository;
  private final InvestWeekRepository weekRepository;
  private final WeeklyResultRepository resultRepository;
  private final InvestAccountRepository accountRepository;
  private final HoldingRepository holdingRepository;

  /** 1위, 2위, 3위가 받는 포인트. 목록 길이를 넘는 순위는 0점이다. */
  @Value("${invest.weekly.points:3,2,1}")
  private List<Integer> points;

  /** @return 이미 정산된 주면 false */
  @Transactional
  public boolean settle(LocalDate weekEnd) {
    if (weekRepository.existsByWeekEndDate(weekEnd)) {
      return false;
    }

    List<UUID> participants = tradeRepository.findParticipantUserIds(weekEnd);
    List<AccountValuation.AccountValue> scored = new ArrayList<>(
      valuation.evaluate(participants)
    );
    scored.sort(
      Comparator
        .comparing(AccountValuation.AccountValue::returnRate)
        .reversed()
        .thenComparing(AccountValuation.AccountValue::userId)
    );

    InvestWeek week = weekRepository.saveAndFlush(
      InvestWeek.of(weekEnd, scored.size())
    );

    List<WeeklyResult> results = new ArrayList<>();
    int position = 0;
    int rank = 0;
    AccountValuation.AccountValue previous = null;
    for (AccountValuation.AccountValue s : scored) {
      position++;
      if (previous == null || previous.returnRate().compareTo(s.returnRate()) != 0) {
        rank = position; // 수익률이 완전히 같으면 같은 순위, 같은 점수
      }
      previous = s;
      results.add(
        WeeklyResult.of(
          week.getId(),
          s.userId(),
          rank,
          s.returnRate().setScale(6, RoundingMode.HALF_UP),
          s.equity(),
          pointsFor(rank)
        )
      );
    }
    resultRepository.saveAll(results);

    int deleted = holdingRepository.deleteAllHoldings();
    int reset = accountRepository.resetAllCash();
    log.info(
      "주간 정산 완료 weekEnd={} 참여={}명 초기화 계좌={} 삭제된 보유={}건",
      weekEnd,
      scored.size(),
      reset,
      deleted
    );
    return true;
  }

  private int pointsFor(int rank) {
    return rank >= 1 && rank <= points.size() ? points.get(rank - 1) : 0;
  }
}
