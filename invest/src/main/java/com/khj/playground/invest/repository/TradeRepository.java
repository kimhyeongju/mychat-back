package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.Trade;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
  Page<Trade> findByAccountIdOrderByCreatedAtDesc(
    UUID accountId,
    Pageable pageable
  );
}
