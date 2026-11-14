package com.khj.playground.invest.dto;

import com.khj.playground.invest.enums.TradeSide;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TradeRequest(
  @NotBlank String symbol,
  @NotNull TradeSide side,
  @Min(1) @Max(1_000_000) long quantity
) {}
