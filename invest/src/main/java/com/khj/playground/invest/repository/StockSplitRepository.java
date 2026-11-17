package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.StockSplit;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockSplitRepository extends JpaRepository<StockSplit, UUID> {
  boolean existsBySymbolAndExDate(String symbol, LocalDate exDate);

  /** 적용 시점이 된(ex_date 가 오늘 이하) 미적용 분할. */
  List<StockSplit> findByAppliedAtIsNullAndExDateLessThanEqualOrderByExDateAsc(
    LocalDate today
  );

  List<StockSplit> findBySymbolAndAppliedAtIsNullAndExDateLessThanEqual(
    String symbol,
    LocalDate today
  );

  /** 적용 중 같은 분할을 동시에 처리하지 못하게 행 잠금을 건다. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM StockSplit s WHERE s.id = :id")
  Optional<StockSplit> findByIdForUpdate(@Param("id") UUID id);
}
