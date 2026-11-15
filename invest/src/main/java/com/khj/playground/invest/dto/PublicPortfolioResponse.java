package com.khj.playground.invest.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 다른 사용자의 포트폴리오. 금액 대신 비중만 공개한다.
 *
 * @param rank 현재 순위. 랭킹에 아직 반영되지 않았으면 null
 * @param cashWeight 현금 비중
 */
public record PublicPortfolioResponse(
  UUID userId,
  String nickname,
  Integer rank,
  BigDecimal returnRate,
  BigDecimal cashWeight,
  List<PublicPositionResponse> positions,
  boolean pricesStale
) {}
