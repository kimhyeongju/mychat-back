package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.StockPostLike;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StockPostLikeRepository extends JpaRepository<StockPostLike, Long> {
  boolean existsByPostIdAndUserId(Long postId, UUID userId);

  /** 주어진 글 중 이 사용자가 좋아요한 글의 id. */
  @Query("SELECT l.postId FROM StockPostLike l WHERE l.userId = :userId AND l.postId IN :postIds")
  List<Long> findLikedPostIds(
    @Param("userId") UUID userId,
    @Param("postIds") Collection<Long> postIds
  );

  @Query("SELECT l.postId FROM StockPostLike l WHERE l.userId = :userId")
  List<Long> findPostIdsByUserId(@Param("userId") UUID userId);

  @Transactional
  @Modifying
  @Query("DELETE FROM StockPostLike l WHERE l.postId = :postId AND l.userId = :userId")
  int deleteByPostIdAndUserId(
    @Param("postId") Long postId,
    @Param("userId") UUID userId
  );

  @Transactional
  @Modifying
  @Query("DELETE FROM StockPostLike l WHERE l.userId = :userId")
  int deleteAllByUserId(@Param("userId") UUID userId);
}
