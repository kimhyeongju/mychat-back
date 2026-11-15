package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.InvestWeek;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestWeekRepository extends JpaRepository<InvestWeek, Long> {
  boolean existsByWeekEndDate(LocalDate weekEndDate);

  Optional<InvestWeek> findFirstByOrderByWeekEndDateDesc();
}
