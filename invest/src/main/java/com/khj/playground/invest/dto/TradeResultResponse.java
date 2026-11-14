package com.khj.playground.invest.dto;

import java.math.BigDecimal;

/** 주문 체결 결과. 화면이 잔고를 바로 갱신할 수 있도록 체결 후 현금을 함께 준다. */
public record TradeResultResponse(TradeResponse trade, BigDecimal cashAfter) {}
