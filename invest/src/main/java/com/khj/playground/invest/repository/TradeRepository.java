package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.Trade;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
  Page<Trade> findByAccountIdOrderByCreatedAtDesc(
    UUID accountId,
    Pageable pageable
  );

  /** 그 주에 한 번이라도 거래한 사용자. 주간 랭킹과 포인트 대상이다. */
  @Query(
    "SELECT DISTINCT t.account.userId FROM Trade t WHERE t.weekEndDate = :weekEnd"
  )
  List<UUID> findParticipantUserIds(@Param("weekEnd") LocalDate weekEnd);

  /** 분할 조정 대상 종목 선정용. 최근에 거래된 종목. */
  @Query("SELECT DISTINCT t.symbol FROM Trade t WHERE t.createdAt >= :since")
  List<String> findSymbolsTradedSince(@Param("since") LocalDateTime since);

  /** 늦게 알게 된 분할에서 "분할 후 거래가 섞였는지" 확인한다. */
  boolean existsByAccountIdAndSymbolAndCreatedAtGreaterThanEqual(
    UUID accountId,
    String symbol,
    LocalDateTime since
  );
}
