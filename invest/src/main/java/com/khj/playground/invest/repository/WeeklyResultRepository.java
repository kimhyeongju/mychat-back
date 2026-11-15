package com.khj.playground.invest.repository;

import com.khj.playground.invest.entity.WeeklyResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WeeklyResultRepository extends JpaRepository<WeeklyResult, Long> {
  /**
   * 사용자별 누적 집계. 각 행은 [userId, 총 포인트, 1위 횟수, 2위 횟수, 3위 횟수, 참여 주 수].
   * 포인트가 있는 사용자만 돌려준다.
   */
  @Query(
    """
    SELECT r.userId,
           SUM(r.points),
           SUM(CASE WHEN r.rankNo = 1 THEN 1 ELSE 0 END),
           SUM(CASE WHEN r.rankNo = 2 THEN 1 ELSE 0 END),
           SUM(CASE WHEN r.rankNo = 3 THEN 1 ELSE 0 END),
           COUNT(r)
    FROM WeeklyResult r
    GROUP BY r.userId
    HAVING SUM(r.points) > 0
    """
  )
  List<Object[]> aggregateByUser();

  /** 한 사용자의 누적 집계(포인트가 0이어도 참여 기록은 센다). 결과는 [총 포인트, 1위, 2위, 3위, 참여 주 수]. */
  @Query(
    """
    SELECT COALESCE(SUM(r.points), 0),
           COALESCE(SUM(CASE WHEN r.rankNo = 1 THEN 1 ELSE 0 END), 0),
           COALESCE(SUM(CASE WHEN r.rankNo = 2 THEN 1 ELSE 0 END), 0),
           COALESCE(SUM(CASE WHEN r.rankNo = 3 THEN 1 ELSE 0 END), 0),
           COUNT(r)
    FROM WeeklyResult r
    WHERE r.userId = :userId
    """
  )
  List<Object[]> summarize(@org.springframework.data.repository.query.Param("userId") UUID userId);

  List<WeeklyResult> findTop10ByUserIdOrderByWeekIdDesc(UUID userId);

  List<WeeklyResult> findByWeekIdAndRankNoLessThanEqualOrderByRankNoAscIdAsc(
    Long weekId,
    int rankNo
  );
}
