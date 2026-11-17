package com.khj.playground.invest.dto;

import com.khj.playground.invest.enums.TradeSide;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TradeRequest(
  @NotBlank(message = "{invest-dto.symbol.not-blank}") String symbol,
  @NotNull(message = "{invest-dto.side.not-null}") TradeSide side,
  @Min(value = 1, message = "{invest-dto.quantity.min}")
  @Max(value = 1_000_000, message = "{invest-dto.quantity.max}")
  long quantity
) {}
