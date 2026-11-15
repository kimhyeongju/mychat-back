package com.khj.playground.invest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 정산이 끝난 한 주. */
@Getter
@Entity
@Table(name = "invest_weeks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestWeek {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "week_end_date", nullable = false)
  private LocalDate weekEndDate;

  @Column(name = "participant_count", nullable = false)
  private int participantCount;

  @Column(name = "settled_at", nullable = false)
  private LocalDateTime settledAt;

  public static InvestWeek of(LocalDate weekEndDate, int participantCount) {
    InvestWeek week = new InvestWeek();
    week.weekEndDate = weekEndDate;
    week.participantCount = participantCount;
    week.settledAt = LocalDateTime.now();
    return week;
  }
}
