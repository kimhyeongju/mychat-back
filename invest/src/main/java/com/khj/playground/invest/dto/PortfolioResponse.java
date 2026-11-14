package com.khj.playground.invest.dto;

import java.util.List;

public record PortfolioResponse(
  AccountSummaryResponse summary,
  List<PositionResponse> positions
) {}
