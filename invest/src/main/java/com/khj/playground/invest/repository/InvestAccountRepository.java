package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.InvestAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvestAccountRepository
  extends JpaRepository<InvestAccount, UUID> {
  Optional<InvestAccount> findByUserId(UUID userId);

  /**
   * 주문 처리용. 같은 계좌에 동시에 들어온 주문이 잔고를 음수로 만들지 못하도록
   * 행 잠금(SELECT ... FOR UPDATE)을 건다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM InvestAccount a WHERE a.userId = :userId")
  Optional<InvestAccount> findByUserIdForUpdate(@Param("userId") UUID userId);

  /** 분할 적용용. 주문과 같은 행 잠금을 건다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM InvestAccount a WHERE a.id = :id")
  Optional<InvestAccount> lockById(@Param("id") UUID id);

  /** 주간 초기화. 모든 계좌의 현금을 시작 자금으로 되돌린다. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE InvestAccount a SET a.cash = a.seedMoney")
  int resetAllCash();
}
