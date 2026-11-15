package com.khj.playground.invest.service;

import com.khj.playground.invest.entity.Holding;
import com.khj.playground.invest.entity.InvestAccount;
import com.khj.playground.invest.repository.HoldingRepository;
import com.khj.playground.invest.repository.InvestAccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 여러 계좌의 총 자산과 수익률을 한꺼번에 계산한다. 랭킹과 주간 정산이 같은 계산을 쓴다.
 * 시세가 없는 종목은 평균단가로 평가한다.
 */
@Service
@RequiredArgsConstructor
public class AccountValuation {

  private final InvestAccountRepository accountRepository;
  private final HoldingRepository holdingRepository;
  private final PriceService priceService;

  /** @param returnRate 소수 10자리. 순위 비교에 쓰며 화면에는 더 짧게 자른다. */
  public record AccountValue(
    UUID userId,
    UUID accountId,
    BigDecimal equity,
    BigDecimal returnRate
  ) {}

  /** @param onlyUsers null 이면 전체 계좌 */
  @Transactional(readOnly = true)
  public List<AccountValue> evaluate(Collection<UUID> onlyUsers) {
    Set<UUID> filter = onlyUsers == null ? null : Set.copyOf(onlyUsers);

    Map<UUID, BigDecimal> holdingsByAccount = new HashMap<>();
    Map<String, BigDecimal> priceCache = new HashMap<>();
    for (Holding h : holdingRepository.findAllWithAccount()) {
      if (filter != null && !filter.contains(h.getAccount().getUserId())) {
        continue;
      }
      BigDecimal price = priceCache.computeIfAbsent(
        h.getSymbol(),
        s ->
          priceService
            .findQuoteForValuation(s)
            .map(v -> v.quote().price())
            .orElse(null)
      );
      BigDecimal effective = price != null ? price : h.getAvgCost();
      holdingsByAccount.merge(
        h.getAccount().getId(),
        effective.multiply(BigDecimal.valueOf(h.getQuantity())),
        BigDecimal::add
      );
    }

    List<AccountValue> result = new ArrayList<>();
    for (InvestAccount account : accountRepository.findAll()) {
      if (filter != null && !filter.contains(account.getUserId())) {
        continue;
      }
      BigDecimal equity = account
        .getCash()
        .add(holdingsByAccount.getOrDefault(account.getId(), BigDecimal.ZERO))
        .setScale(2, RoundingMode.HALF_UP);
      BigDecimal rate = equity
        .subtract(account.getSeedMoney())
        .divide(account.getSeedMoney(), 10, RoundingMode.HALF_UP);
      result.add(new AccountValue(account.getUserId(), account.getId(), equity, rate));
    }
    return result;
  }
}
