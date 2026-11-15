package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.SymbolAlias;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SymbolAliasRepository extends JpaRepository<SymbolAlias, Long> {
  List<SymbolAlias> findAllByOrderByIdAsc();
}
