package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.StockPost;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StockPostRepository extends JpaRepository<StockPost, Long> {
  Page<StockPost> findBySymbol(String symbol, Pageable pageable);

  /** 좋아요 건수를 실제 행 수로 다시 맞춘다. 여러 번 호출해도 결과가 같다. */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE StockPost p SET p.likeCount = (SELECT CAST(COUNT(l) AS Integer) FROM StockPostLike l WHERE l.postId = p.id) WHERE p.id = :postId"
  )
  int recountLikes(@Param("postId") Long postId);

  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "UPDATE StockPost p SET p.commentCount = (SELECT CAST(COUNT(c) AS Integer) FROM StockComment c WHERE c.postId = p.id) WHERE p.id = :postId"
  )
  int recountComments(@Param("postId") Long postId);

  /** 탈퇴 회원의 글. 댓글·좋아요는 FK(ON DELETE CASCADE)로 함께 지워진다. */
  @Transactional
  @Modifying
  @Query("DELETE FROM StockPost p WHERE p.userId = :userId")
  int deleteAllByUserId(@Param("userId") UUID userId);
}
