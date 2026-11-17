package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.SplitAdjustment;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SplitAdjustmentRepository
  extends JpaRepository<SplitAdjustment, UUID> {
  List<SplitAdjustment> findByAccountIdAndWeekEndDateOrderByCreatedAtDesc(
    UUID accountId,
    LocalDate weekEndDate
  );
}
