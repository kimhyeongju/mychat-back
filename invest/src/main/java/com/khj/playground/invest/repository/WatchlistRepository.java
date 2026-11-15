package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.WatchlistItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
  List<WatchlistItem> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId);

  boolean existsByUserIdAndSymbol(UUID userId, String symbol);

  long countByUserId(UUID userId);

  @Modifying
  @Query("DELETE FROM WatchlistItem w WHERE w.userId = :userId AND w.symbol = :symbol")
  int deleteByUserIdAndSymbol(
    @Param("userId") UUID userId,
    @Param("symbol") String symbol
  );

  /** 누군가 관심 종목으로 둔 심볼 전체. 시세 주기 갱신 대상에 포함한다. */
  @Query("SELECT DISTINCT w.symbol FROM WatchlistItem w")
  List<String> findDistinctSymbols();
}
