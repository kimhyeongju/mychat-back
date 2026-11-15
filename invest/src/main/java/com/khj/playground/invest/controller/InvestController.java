package com.khj.playground.invest.controller;

import com.khj.playground.common.dto.PageResponse;
import com.khj.playground.common.security.CurrentUserProvider;
import com.khj.playground.invest.dto.AccountSummaryResponse;
import com.khj.playground.invest.dto.FxResponse;
import com.khj.playground.invest.dto.PortfolioResponse;
import com.khj.playground.invest.dto.PublicPortfolioResponse;
import com.khj.playground.invest.dto.QuoteResponse;
import com.khj.playground.invest.dto.RankingResponse;
import com.khj.playground.invest.dto.SymbolSearchResponse;
import com.khj.playground.invest.dto.TradeRequest;
import com.khj.playground.invest.dto.TradeResponse;
import com.khj.playground.invest.dto.TradeResultResponse;
import com.khj.playground.invest.service.FxService;
import com.khj.playground.invest.service.PortfolioService;
import com.khj.playground.invest.service.PriceService;
import com.khj.playground.invest.service.RankingService;
import com.khj.playground.invest.service.SymbolService;
import com.khj.playground.invest.service.TradeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/invest")
@RequiredArgsConstructor
public class InvestController {

  private final PriceService priceService;
  private final SymbolService symbolService;
  private final FxService fxService;
  private final RankingService rankingService;
  private final PortfolioService portfolioService;
  private final TradeService tradeService;
  private final CurrentUserProvider currentUserProvider;

  // ----- 공개 -----

  @GetMapping("/symbols/search")
  public List<SymbolSearchResponse> search(@RequestParam("q") String query) {
    return symbolService
      .search(query)
      .stream()
      .map(SymbolSearchResponse::from)
      .toList();
  }

  @GetMapping("/symbols/{symbol}")
  public QuoteResponse quote(@PathVariable("symbol") String rawSymbol) {
    String symbol = priceService.normalize(rawSymbol);
    PriceService.QuoteView view = priceService.getQuote(symbol);
    return QuoteResponse.of(
      view.quote(),
      symbolService.resolveName(symbol),
      view.stale(),
      priceService.isMarketOpen()
    );
  }

  @GetMapping("/fx")
  public FxResponse fx() {
    return fxService.getUsdKrw();
  }

  /** 비로그인도 볼 수 있다. 로그인 상태면 본인 순위를 함께 준다. */
  @GetMapping("/ranking")
  public RankingResponse ranking(Authentication authentication) {
    return rankingService.getRanking(optionalUserId(authentication));
  }

  // ----- 회원 -----

  @GetMapping("/account")
  public AccountSummaryResponse account(Authentication authentication) {
    return portfolioService.getSummary(userId(authentication));
  }

  @GetMapping("/portfolio")
  public PortfolioResponse portfolio(Authentication authentication) {
    return portfolioService.getPortfolio(userId(authentication));
  }

  /** 다른 사용자의 포트폴리오(비중만). 로그인한 사용자만 볼 수 있다. */
  @GetMapping("/users/{userId}/portfolio")
  public PublicPortfolioResponse userPortfolio(
    @PathVariable("userId") UUID targetUserId,
    Authentication authentication
  ) {
    userId(authentication); // 인증 확인
    return portfolioService.getPublicPortfolio(targetUserId);
  }

  @GetMapping("/trades")
  public PageResponse<TradeResponse> trades(
    @PageableDefault(size = 20) Pageable pageable,
    Authentication authentication
  ) {
    return tradeService.history(userId(authentication), pageable);
  }

  @PostMapping("/trades")
  @ResponseStatus(HttpStatus.CREATED)
  public TradeResultResponse trade(
    @Valid @RequestBody TradeRequest request,
    Authentication authentication
  ) {
    return tradeService.execute(userId(authentication), request);
  }

  private UUID userId(Authentication authentication) {
    return currentUserProvider.resolve(authentication).id();
  }

  private UUID optionalUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    try {
      return currentUserProvider.resolve(authentication).id();
    } catch (ResponseStatusException e) {
      return null;
    }
  }
}
