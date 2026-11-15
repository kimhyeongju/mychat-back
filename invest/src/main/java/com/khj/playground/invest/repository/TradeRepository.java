package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.Trade;
import java.time.LocalDate;
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
}
