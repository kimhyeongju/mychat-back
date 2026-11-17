package com.khj.playground.invest.service;

import com.khj.playground.common.exception.ApiException;
import com.khj.playground.invest.exception.InvestErrorCode;
import com.khj.playground.invest.dto.WatchlistItemResponse;
import com.khj.playground.invest.entity.WatchlistItem;
import com.khj.playground.invest.market.Quote;
import com.khj.playground.invest.repository.WatchlistRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관심 종목. 추가·삭제는 여러 번 해도 결과가 같다(멱등). */
@Service
@RequiredArgsConstructor
public class WatchlistService {

  static final int MAX_ITEMS = 50;

  private final WatchlistRepository watchlistRepository;
  private final PriceService priceService;
  private final SymbolService symbolService;

  /** 최근에 추가한 순. 시세는 캐시 값을 쓰고, 보유·관심 종목은 주기 갱신이 최신으로 유지한다. */
  @Transactional(readOnly = true)
  public List<WatchlistItemResponse> list(UUID userId) {
    List<WatchlistItemResponse> result = new ArrayList<>();
    for (WatchlistItem item : watchlistRepository.findByUserIdOrderByCreatedAtDescIdDesc(
      userId
    )) {
      String symbol = item.getSymbol();
      String name = symbolService.findNameOrSymbol(symbol);
      Optional<PriceService.QuoteView> view = priceService.findQuoteForValuation(symbol);
      if (view.isPresent()) {
        Quote q = view.get().quote();
        result.add(
          new WatchlistItemResponse(
            symbol,
            name,
            q.price(),
            q.change(),
            q.changePercent(),
            q.tradedAt(),
            view.get().stale()
          )
        );
      } else {
        result.add(new WatchlistItemResponse(symbol, name, null, null, null, null, true));
      }
    }
    return result;
  }

  /** 일부러 트랜잭션을 걸지 않는다. 동시 중복 저장 예외를 잡아도 롤백 표시가 남지 않게 하기 위해서다. */
  public void add(UUID userId, String rawSymbol) {
    String symbol = priceService.normalize(rawSymbol);
    if (watchlistRepository.existsByUserIdAndSymbol(userId, symbol)) {
      return;
    }
    if (watchlistRepository.countByUserId(userId) >= MAX_ITEMS) {
      throw new ApiException(InvestErrorCode.WATCHLIST_FULL, String.valueOf(MAX_ITEMS));
    }
    // 존재하는 종목인지 확인한다. 없는 심볼이면 404 가 나가고 저장하지 않는다.
    priceService.getQuote(symbol);
    symbolService.resolveName(symbol);

    try {
      watchlistRepository.saveAndFlush(WatchlistItem.of(userId, symbol));
    } catch (DataIntegrityViolationException e) {
      // 동시에 두 번 눌러 이미 들어갔다. 결과는 같으므로 무시한다.
    }
  }

  @Transactional
  public void remove(UUID userId, String rawSymbol) {
    watchlistRepository.deleteByUserIdAndSymbol(
      userId,
      priceService.normalize(rawSymbol)
    );
  }
}
