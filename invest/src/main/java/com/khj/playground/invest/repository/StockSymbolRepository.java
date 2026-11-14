package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.StockSymbol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockSymbolRepository
  extends JpaRepository<StockSymbol, String> {}
