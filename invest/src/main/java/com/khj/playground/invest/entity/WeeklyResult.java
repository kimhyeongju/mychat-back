package com.khj.playground.invest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 한 주의 마감 시점 성적 한 줄. 정산 이후 바뀌지 않는다. */
@Getter
@Entity
@Table(name = "invest_weekly_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "week_id", nullable = false)
  private Long weekId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "rank_no", nullable = false)
  private int rankNo;

  @Column(name = "return_rate", nullable = false, precision = 14, scale = 6)
  private BigDecimal returnRate;

  @Column(name = "final_equity", nullable = false, precision = 18, scale = 2)
  private BigDecimal finalEquity;

  @Column(nullable = false)
  private int points;

  public static WeeklyResult of(
    Long weekId,
    UUID userId,
    int rankNo,
    BigDecimal returnRate,
    BigDecimal finalEquity,
    int points
  ) {
    WeeklyResult r = new WeeklyResult();
    r.weekId = weekId;
    r.userId = userId;
    r.rankNo = rankNo;
    r.returnRate = returnRate;
    r.finalEquity = finalEquity;
    r.points = points;
    return r;
  }
}
