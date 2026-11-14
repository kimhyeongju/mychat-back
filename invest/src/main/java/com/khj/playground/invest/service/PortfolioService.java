package com.khj.playground.invest.service;

import com.khj.playground.invest.dto.AccountSummaryResponse;
import com.khj.playground.invest.dto.PortfolioResponse;
import com.khj.playground.invest.dto.PositionResponse;
import com.khj.playground.invest.entity.Holding;
import com.khj.playground.invest.entity.InvestAccount;
import com.khj.playground.invest.repository.HoldingRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 보유 종목을 현재 시세로 평가한다. 시세가 없으면 평균단가로 대신하고 stale 로 표시한다. */
@Service
@RequiredArgsConstructor
public class PortfolioService {

  private final InvestAccountService accountService;
  private final HoldingRepository holdingRepository;
  private final PriceService priceService;
  private final SymbolService symbolService;

  @Transactional
  public PortfolioResponse getPortfolio(UUID userId) {
    InvestAccount account = accountService.getOrOpen(userId);
    List<Holding> holdings = holdingRepository.findByAccountIdOrderBySymbolAsc(
      account.getId()
    );

    List<PositionResponse> positions = new ArrayList<>();
    BigDecimal holdingsValue = BigDecimal.ZERO;
    boolean anyStale = false;

    for (Holding h : holdings) {
      Optional<PriceService.QuoteView> view = priceService.findQuote(h.getSymbol());
      BigDecimal price = view.map(v -> v.quote().price()).orElse(null);
      boolean stale = view.map(PriceService.QuoteView::stale).orElse(true);
      anyStale |= stale;

      BigDecimal effective = price != null ? price : h.getAvgCost();
      BigDecimal qty = BigDecimal.valueOf(h.getQuantity());
      BigDecimal marketValue = effective
        .multiply(qty)
        .setScale(2, RoundingMode.HALF_UP);
      BigDecimal cost = h.getAvgCost().multiply(qty);
      BigDecimal profit = marketValue.subtract(cost).setScale(2, RoundingMode.HALF_UP);
      BigDecimal returnRate = cost.signum() == 0
        ? BigDecimal.ZERO
        : profit.divide(cost, 6, RoundingMode.HALF_UP);

      holdingsValue = holdingsValue.add(marketValue);
      positions.add(
        new PositionResponse(
          h.getSymbol(),
          symbolService.findNameOrSymbol(h.getSymbol()),
          h.getQuantity(),
          h.getAvgCost(),
          price,
          marketValue,
          profit,
          returnRate,
          stale
        )
      );
    }

    return new PortfolioResponse(
      summarize(account, holdingsValue, anyStale),
      positions
    );
  }

  /** 계좌 요약만 필요할 때. 포트폴리오 전체를 계산한 뒤 요약만 떼어 준다. */
  @Transactional
  public AccountSummaryResponse getSummary(UUID userId) {
    return getPortfolio(userId).summary();
  }

  static AccountSummaryResponse summarize(
    InvestAccount account,
    BigDecimal holdingsValue,
    boolean pricesStale
  ) {
    BigDecimal equity = account.getCash().add(holdingsValue);
    BigDecimal profit = equity.subtract(account.getSeedMoney());
    BigDecimal returnRate = profit.divide(
      account.getSeedMoney(),
      6,
      RoundingMode.HALF_UP
    );
    return new AccountSummaryResponse(
      account.getCash(),
      account.getSeedMoney(),
      holdingsValue.setScale(2, RoundingMode.HALF_UP),
      equity.setScale(2, RoundingMode.HALF_UP),
      profit.setScale(2, RoundingMode.HALF_UP),
      returnRate,
      pricesStale
    );
  }
}
