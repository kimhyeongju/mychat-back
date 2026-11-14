package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.InvestAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
