package com.khj.playground.invest.dto;

import com.khj.playground.invest.market.SymbolInfo;

public record SymbolSearchResponse(String symbol, String name, String exchange) {
  public static SymbolSearchResponse from(SymbolInfo info) {
    return new SymbolSearchResponse(info.symbol(), info.name(), info.exchange());
  }
}
