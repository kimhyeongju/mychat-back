package com.khj.playground.invest.service;

import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.MemberLookup;
import com.khj.playground.invest.dto.LeaderboardEntry;
import com.khj.playground.invest.dto.ProfileResponse;
import com.khj.playground.invest.entity.InvestWeek;
import com.khj.playground.invest.entity.WeeklyResult;
import com.khj.playground.invest.repository.InvestWeekRepository;
import com.khj.playground.invest.repository.WeeklyResultRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 누적 포인트 리더보드와 사용자 프로필. 주간 정산 결과(WeeklyResult)를 집계한다. */
@Service
@RequiredArgsConstructor
public class ScoreService {

  private static final int TOP_N = 50;
  private static final Duration CACHE_TTL = Duration.ofSeconds(30);
  private static final String WITHDRAWN = "(탈퇴한 사용자)";

  private final WeeklyResultRepository resultRepository;
  private final InvestWeekRepository weekRepository;
  private final MemberLookup memberLookup;

  private volatile Board board;

  @Transactional(readOnly = true)
  public List<LeaderboardEntry> leaderboard() {
    return currentBoard().top();
  }

  @Transactional(readOnly = true)
  public ProfileResponse profile(UUID userId) {
    String nickname = memberLookup
      .findById(userId)
      .map(AuthenticatedUser::nickname)
      .orElse(null);

    Object[] sum = resultRepository.summarize(userId).get(0);
    int weeksPlayed = num(sum[4]);
    if (nickname == null && weeksPlayed == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }

    List<WeeklyResult> recent = resultRepository.findTop10ByUserIdOrderByWeekIdDesc(
      userId
    );
    Map<Long, LocalDate> weekDates = weekRepository
      .findAllById(recent.stream().map(WeeklyResult::getWeekId).toList())
      .stream()
      .collect(Collectors.toMap(InvestWeek::getId, InvestWeek::getWeekEndDate));

    return new ProfileResponse(
      userId,
      nickname == null ? WITHDRAWN : nickname,
      num(sum[0]),
      num(sum[1]),
      num(sum[2]),
      num(sum[3]),
      weeksPlayed,
      currentBoard().rankByUser().get(userId),
      recent
        .stream()
        .map(r ->
          new ProfileResponse.WeekRecord(
            weekDates.get(r.getWeekId()),
            r.getRankNo(),
            r.getReturnRate(),
            r.getPoints()
          )
        )
        .toList()
    );
  }

  /** 정산 직후 캐시를 버린다. */
  public void invalidate() {
    board = null;
  }

  private Board currentBoard() {
    Board current = board;
    if (current == null || current.isExpired()) {
      synchronized (this) {
        current = board;
        if (current == null || current.isExpired()) {
          current = compute();
          board = current;
        }
      }
    }
    return current;
  }

  private Board compute() {
    record Row(UUID userId, int points, int first, int second, int third, int weeks) {}

    List<Row> rows = new ArrayList<>();
    for (Object[] r : resultRepository.aggregateByUser()) {
      rows.add(
        new Row((UUID) r[0], num(r[1]), num(r[2]), num(r[3]), num(r[4]), num(r[5]))
      );
    }
    // 포인트 → 1위 횟수 → 2위 횟수 → 3위 횟수. 전부 같으면 같은 순위.
    rows.sort(
      Comparator
        .comparingInt(Row::points)
        .thenComparingInt(Row::first)
        .thenComparingInt(Row::second)
        .thenComparingInt(Row::third)
        .reversed()
        .thenComparing(Row::userId)
    );

    List<LeaderboardEntry> top = new ArrayList<>();
    Map<UUID, Integer> rankByUser = new HashMap<>();
    int position = 0;
    int rank = 0;
    Row previous = null;
    for (Row row : rows) {
      position++;
      if (
        previous == null ||
        previous.points() != row.points() ||
        previous.first() != row.first() ||
        previous.second() != row.second() ||
        previous.third() != row.third()
      ) {
        rank = position;
      }
      previous = row;
      rankByUser.put(row.userId(), rank);
      if (position <= TOP_N) {
        top.add(
          new LeaderboardEntry(
            rank,
            row.userId(),
            memberLookup
              .findById(row.userId())
              .map(AuthenticatedUser::nickname)
              .orElse(WITHDRAWN),
            row.points(),
            row.first(),
            row.second(),
            row.third(),
            row.weeks()
          )
        );
      }
    }
    return new Board(List.copyOf(top), Map.copyOf(rankByUser), Instant.now());
  }

  private static int num(Object value) {
    return value == null ? 0 : ((Number) value).intValue();
  }

  private record Board(
    List<LeaderboardEntry> top,
    Map<UUID, Integer> rankByUser,
    Instant computedAt
  ) {
    boolean isExpired() {
      return Duration.between(computedAt, Instant.now()).compareTo(CACHE_TTL) > 0;
    }
  }
}
