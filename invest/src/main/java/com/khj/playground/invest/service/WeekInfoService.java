package com.khj.playground.invest.service;

import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.MemberLookup;
import com.khj.playground.invest.dto.WeekInfoResponse;
import com.khj.playground.invest.entity.InvestWeek;
import com.khj.playground.invest.repository.InvestWeekRepository;
import com.khj.playground.invest.repository.WeeklyResultRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이번 주 일정, 규칙, 지난주 결과. 화면의 카운트다운과 "?" 설명이 쓴다. */
@Service
@RequiredArgsConstructor
public class WeekInfoService {

  private final WeeklyPeriod weeklyPeriod;
  private final InvestWeekRepository weekRepository;
  private final WeeklyResultRepository resultRepository;
  private final MemberLookup memberLookup;

  @Value("${invest.seed-money}")
  private BigDecimal seedMoney;

  @Value("${invest.weekly.points:3,2,1}")
  private List<Integer> points;

  @Transactional(readOnly = true)
  public WeekInfoResponse getInfo() {
    Optional<InvestWeek> last = weekRepository.findFirstByOrderByWeekEndDateDesc();
    WeekInfoResponse.LastWeek lastWeek = last
      .map(w ->
        new WeekInfoResponse.LastWeek(
          w.getWeekEndDate(),
          w.getParticipantCount(),
          resultRepository
            .findByWeekIdAndRankNoLessThanEqualOrderByRankNoAscIdAsc(
              w.getId(),
              points.size()
            )
            .stream()
            .filter(r -> r.getPoints() > 0)
            .map(r ->
              new WeekInfoResponse.Winner(
                r.getRankNo(),
                r.getUserId(),
                memberLookup
                  .findById(r.getUserId())
                  .map(AuthenticatedUser::nickname)
                  .orElse("(탈퇴한 사용자)"),
                r.getReturnRate(),
                r.getPoints()
              )
            )
            .toList()
        )
      )
      .orElse(null);

    return new WeekInfoResponse(
      weeklyPeriod.currentWeekEnd(),
      weeklyPeriod.currentSettlementAt(),
      seedMoney,
      points,
      lastWeek
    );
  }
}
