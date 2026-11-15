package com.khj.playground.invest.service;

import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.MemberLookup;
import com.khj.playground.invest.dto.RankingEntry;
import com.khj.playground.invest.dto.RankingResponse;
import com.khj.playground.invest.entity.Holding;
import com.khj.playground.invest.entity.InvestAccount;
import com.khj.playground.invest.repository.HoldingRepository;
import com.khj.playground.invest.repository.InvestAccountRepository;
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
 * 수익률 순위. 전체 계좌를 평가해 정렬한다.
 * 계산 비용이 있어 20초 동안 메모리에 캐시한다. 단일 인스턴스를 전제로 한다.
 */
@Service
@RequiredArgsConstructor
public class RankingService {

  private static final int TOP_N = 50;
  private static final Duration CACHE_TTL = Duration.ofSeconds(20);

  private final InvestAccountRepository accountRepository;
  private final HoldingRepository holdingRepository;
  private final PriceService priceService;
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

  /** 특정 사용자의 현재 순위 항목. 계좌가 없으면 비어 있다. */
  @Transactional(readOnly = true)
  public Optional<RankingEntry> findEntry(UUID userId) {
    return Optional.ofNullable(currentSnapshot().byUser().get(userId));
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
    List<InvestAccount> accounts = accountRepository.findAll();
    Map<UUID, BigDecimal> holdingsValueByAccount = new HashMap<>();
    Map<String, BigDecimal> priceCache = new HashMap<>();

    for (Holding h : holdingRepository.findAllWithAccount()) {
      BigDecimal price = priceCache.computeIfAbsent(
        h.getSymbol(),
        s -> priceService.findQuoteForValuation(s).map(v -> v.quote().price()).orElse(null)
      );
      BigDecimal effective = price != null ? price : h.getAvgCost();
      BigDecimal value = effective.multiply(BigDecimal.valueOf(h.getQuantity()));
      holdingsValueByAccount.merge(h.getAccount().getId(), value, BigDecimal::add);
    }

    List<Scored> scored = new ArrayList<>();
    for (InvestAccount account : accounts) {
      BigDecimal equity = account
        .getCash()
        .add(holdingsValueByAccount.getOrDefault(account.getId(), BigDecimal.ZERO));
      BigDecimal returnRate = equity
        .subtract(account.getSeedMoney())
        .divide(account.getSeedMoney(), 6, RoundingMode.HALF_UP);
      scored.add(new Scored(account.getUserId(), equity, returnRate));
    }
    scored.sort(
      Comparator
        .comparing(Scored::returnRate)
        .reversed()
        .thenComparing(Scored::userId)
    );

    List<RankingEntry> top = new ArrayList<>();
    Map<UUID, RankingEntry> byUser = new HashMap<>();
    int rank = 0;
    for (Scored s : scored) {
      rank++;
      // 상위권만 닉네임을 조회한다. 나머지는 본인 순위 표시에만 쓰여 닉네임이 필요 없다.
      String nickname = rank <= TOP_N
        ? memberLookup
          .findById(s.userId())
          .map(AuthenticatedUser::nickname)
          .orElse("(탈퇴한 사용자)")
        : null;
      RankingEntry entry = new RankingEntry(
        rank,
        s.userId(),
        nickname,
        s.equity().setScale(2, RoundingMode.HALF_UP),
        s.returnRate()
      );
      byUser.put(s.userId(), entry);
      if (rank <= TOP_N) {
        top.add(entry);
      }
    }

    return new Snapshot(List.copyOf(top), Map.copyOf(byUser), Instant.now());
  }

  private record Scored(UUID userId, BigDecimal equity, BigDecimal returnRate) {}

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
