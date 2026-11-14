package com.khj.playground.invest.dto;

import java.time.Instant;
import java.util.List;

/**
 * @param entries 상위 N명
 * @param me 로그인한 사용자의 순위. 계좌가 없거나 비로그인이면 null
 * @param totalParticipants 계좌를 개설한 전체 인원
 */
public record RankingResponse(
  List<RankingEntry> entries,
  RankingEntry me,
  int totalParticipants,
  Instant computedAt
) {}
