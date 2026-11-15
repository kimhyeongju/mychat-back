package com.khj.playground.invest.dto;

import com.khj.playground.invest.entity.Trade;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TradeResponse(
  UUID id,
  String symbol,
  String name,
  String side,
  long quantity,
  BigDecimal price,
  BigDecimal amount,
  LocalDateTime createdAt,
  LocalDate weekEndDate
) {
  public static TradeResponse from(Trade trade, String name) {
    return new TradeResponse(
      trade.getId(),
      trade.getSymbol(),
      name,
      trade.getSide().name(),
      trade.getQuantity(),
      trade.getPrice(),
      trade.getAmount(),
      trade.getCreatedAt(),
      trade.getWeekEndDate()
    );
  }
}
