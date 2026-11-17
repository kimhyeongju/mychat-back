package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.StockComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StockCommentRepository extends JpaRepository<StockComment, Long> {
  /** 오래된 순. */
  List<StockComment> findByPostIdOrderByIdAsc(Long postId, Pageable pageable);

  @Query("SELECT DISTINCT c.postId FROM StockComment c WHERE c.userId = :userId")
  List<Long> findPostIdsByUserId(@Param("userId") UUID userId);

  @Transactional
  @Modifying
  @Query("DELETE FROM StockComment c WHERE c.userId = :userId")
  int deleteAllByUserId(@Param("userId") UUID userId);
}
