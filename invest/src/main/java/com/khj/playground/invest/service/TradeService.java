package com.khj.playground.invest.service;

import com.khj.playground.common.dto.PageResponse;
import com.khj.playground.invest.dto.TradeRequest;
import com.khj.playground.invest.dto.TradeResponse;
import com.khj.playground.invest.dto.TradeResultResponse;
import com.khj.playground.invest.entity.Holding;
import com.khj.playground.invest.entity.InvestAccount;
import com.khj.playground.invest.entity.Trade;
import com.khj.playground.invest.enums.TradeSide;
import com.khj.playground.invest.market.Quote;
import com.khj.playground.invest.repository.HoldingRepository;
import com.khj.playground.invest.repository.TradeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 시장가 주문. 캐시된 최신 시세로 즉시 체결한다.
 * 수수료 없음, 정수 주 단위, 공매도 불가.
 */
@Service
@RequiredArgsConstructor
public class TradeService {

  private final InvestAccountService accountService;
  private final HoldingRepository holdingRepository;
  private final TradeRepository tradeRepository;
  private final PriceService priceService;
  private final SymbolService symbolService;

  @Transactional
  public TradeResultResponse execute(UUID userId, TradeRequest request) {
    String symbol = priceService.normalize(request.symbol());

    // 시세는 잠금 전에 받는다. 외부 호출 중에 행 잠금을 들고 있지 않기 위해서다.
    Quote quote = priceService.getQuoteForTrade(symbol);
    BigDecimal price = quote.price();
    BigDecimal amount = price
      .multiply(BigDecimal.valueOf(request.quantity()))
      .setScale(2, RoundingMode.HALF_UP);

    InvestAccount account = accountService.lockForTrade(userId);
    Holding holding = holdingRepository
      .findByAccountIdAndSymbol(account.getId(), symbol)
      .orElse(null);

    if (request.side() == TradeSide.BUY) {
      if (account.getCash().compareTo(amount) < 0) {
        throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "현금이 부족합니다. 필요 금액 $" + amount.toPlainString()
        );
      }
      account.withdrawCash(amount);
      if (holding == null) {
        holding = holdingRepository.save(Holding.open(account, symbol));
      }
      holding.buy(request.quantity(), price);
    } else {
      if (holding == null || holding.getQuantity() < request.quantity()) {
        long owned = holding == null ? 0 : holding.getQuantity();
        throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "보유 수량이 부족합니다. 보유 " + owned + "주"
        );
      }
      holding.sell(request.quantity());
      account.depositCash(amount);
      if (holding.isEmpty()) {
        holdingRepository.delete(holding);
      }
    }

    Trade trade = tradeRepository.save(
      Trade.of(account, symbol, request.side(), request.quantity(), price)
    );
    String name = symbolService.resolveName(symbol);

    return new TradeResultResponse(
      TradeResponse.from(trade, name),
      account.getCash()
    );
  }

  // 계좌가 없으면 getOrOpen 이 새로 만들므로 읽기 전용으로 두지 않는다.
  @Transactional
  public PageResponse<TradeResponse> history(UUID userId, Pageable pageable) {
    InvestAccount account = accountService.getOrOpen(userId);
    return PageResponse.of(
      tradeRepository.findByAccountIdOrderByCreatedAtDesc(
        account.getId(),
        pageable
      ),
      trade ->
        TradeResponse.from(
          trade,
          symbolService.findNameOrSymbol(trade.getSymbol())
        )
    );
  }
}
