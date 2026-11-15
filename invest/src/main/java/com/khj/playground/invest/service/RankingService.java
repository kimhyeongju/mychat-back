package com.khj.playground.invest.service;

import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.MemberLookup;
import com.khj.playground.invest.dto.RankingEntry;
import com.khj.playground.invest.dto.RankingResponse;
import com.khj.playground.invest.repository.TradeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이번 주 수익률 순위. 이번 주에 한 번이라도 거래한 사용자만 순위에 오른다(주간 포인트 대상과 같다).
 * 계산 비용이 있어 20초 동안 메모리에 캐시한다. 단일 인스턴스를 전제로 한다.
 * 수익률이 같으면 같은 순위로 묶는다(1, 1, 3 ...).
 */
@Service
@RequiredArgsConstructor
public class RankingService {

  private static final int TOP_N = 50;
  private static final Duration CACHE_TTL = Duration.ofSeconds(20);

  private final AccountValuation valuation;
  private final TradeRepository tradeRepository;
  private final WeeklyPeriod weeklyPeriod;
  private final MemberLookup memberLookup;

  private volatile Snapshot snapshot;

  @Transactional(readOnly = true)
  public RankingResponse getRanking(UUID viewerId) {
    Snapshot current = currentSnapshot();

    RankingEntry me = viewerId == null
      ? null
      : current.byUser().get(viewerId);

    return new RankingResponse(
      current.top(),
      me,
      current.byUser().size(),
      current.computedAt()
    );
  }

  /** 특정 사용자의 이번 주 순위. 이번 주에 거래하지 않았으면 비어 있다. */
  @Transactional(readOnly = true)
  public Optional<RankingEntry> findEntry(UUID userId) {
    return Optional.ofNullable(currentSnapshot().byUser().get(userId));
  }

  /** 주간 정산 직후 이전 주 캐시를 버린다. */
  public void invalidate() {
    snapshot = null;
  }

  private Snapshot currentSnapshot() {
    Snapshot current = snapshot;
    if (current == null || current.isExpired()) {
      synchronized (this) {
        current = snapshot;
        if (current == null || current.isExpired()) {
          current = compute();
          snapshot = current;
        }
      }
    }
    return current;
  }

  private Snapshot compute() {
    List<UUID> participants = tradeRepository.findParticipantUserIds(
      weeklyPeriod.currentWeekEnd()
    );
    List<AccountValuation.AccountValue> scored = new ArrayList<>(
      valuation.evaluate(participants)
    );
    scored.sort(
      Comparator
        .comparing(AccountValuation.AccountValue::returnRate)
        .reversed()
        .thenComparing(AccountValuation.AccountValue::userId)
    );

    List<RankingEntry> top = new ArrayList<>();
    Map<UUID, RankingEntry> byUser = new HashMap<>();
    int position = 0;
    int rank = 0;
    BigDecimal previousRate = null;
    for (AccountValuation.AccountValue s : scored) {
      position++;
      if (previousRate == null || previousRate.compareTo(s.returnRate()) != 0) {
        rank = position; // 동률이면 앞 사람과 같은 순위
      }
      previousRate = s.returnRate();

      // 상위권만 닉네임을 조회한다. 나머지는 본인 순위 표시에만 쓰여 닉네임이 필요 없다.
      String nickname = position <= TOP_N
        ? memberLookup
          .findById(s.userId())
          .map(AuthenticatedUser::nickname)
          .orElse("(탈퇴한 사용자)")
        : null;
      RankingEntry entry = new RankingEntry(
        rank,
        s.userId(),
        nickname,
        s.equity(),
        s.returnRate().setScale(6, RoundingMode.HALF_UP)
      );
      byUser.put(s.userId(), entry);
      if (position <= TOP_N) {
        top.add(entry);
      }
    }

    return new Snapshot(List.copyOf(top), Map.copyOf(byUser), Instant.now());
  }

  private record Snapshot(
    List<RankingEntry> top,
    Map<UUID, RankingEntry> byUser,
    Instant computedAt
  ) {
    boolean isExpired() {
      return Duration.between(computedAt, Instant.now()).compareTo(CACHE_TTL) > 0;
    }
  }
}
