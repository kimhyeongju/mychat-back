package com.khj.playground.invest.market;

import java.math.BigDecimal;
import java.util.Optional;

/** USD→KRW 환율 조회. 표시용이므로 하루 한 번 갱신되는 고시 환율이면 충분하다. */
public interface FxRateClient {
  Optional<BigDecimal> fetchUsdKrw();
}
