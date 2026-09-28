package com.khj.playground.board.repository;

import com.khj.playground.board.entity.Post;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {
  Page<Post> findByBoardIdOrderByCreatedAtDesc(UUID boardId, Pageable pageable);

  Page<Post> findByBoardIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
    UUID boardId,
    String keyword,
    Pageable pageable
  );

  /**
   * 엔티티를 읽어서 +1 하지 않고 DB에서 직접 증가시킨다.
   * 동시 조회 시 값이 덮어써지는 문제를 피할 수 있다.
   */
  @Modifying
  @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
  void increaseViewCount(@Param("id") UUID id);

  @Modifying
  @Query(
    "UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :id"
  )
  void increaseCommentCount(@Param("id") UUID id);

  /** 음수가 되지 않도록 방어한다. */
  @Modifying
  @Query(
    "UPDATE Post p SET p.commentCount = p.commentCount - 1 WHERE p.id = :id AND p.commentCount > 0"
  )
  void decreaseCommentCount(@Param("id") UUID id);
}
