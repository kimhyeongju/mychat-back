package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.Holding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
  List<Holding> findByAccountIdOrderBySymbolAsc(UUID accountId);

  Optional<Holding> findByAccountIdAndSymbol(UUID accountId, String symbol);

  /** 누군가 보유 중인 종목 전체. 주기적 시세 갱신 대상이다. */
  @Query("SELECT DISTINCT h.symbol FROM Holding h")
  List<String> findDistinctSymbols();

  /** 랭킹 계산용. 계좌별로 묶지 않고 한 번에 가져와 메모리에서 합산한다. */
  @Query("SELECT h FROM Holding h JOIN FETCH h.account")
  List<Holding> findAllWithAccount();

  /** 주간 초기화. 모든 보유 종목을 지운다. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM Holding h")
  int deleteAllHoldings();
}
