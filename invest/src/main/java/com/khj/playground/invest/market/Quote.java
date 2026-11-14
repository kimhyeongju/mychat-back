package com.khj.playground.invest.market;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 종목 하나의 시세 스냅샷. 시세 제공자가 무엇이든 이 형태로 통일한다.
 * change/changePercent 는 제공자가 주지 않으면 null 일 수 있다.
 */
public record Quote(
  String symbol,
  BigDecimal price,
  BigDecimal change,
  BigDecimal changePercent,
  BigDecimal previousClose,
  BigDecimal high,
  BigDecimal low,
  BigDecimal open,
  Instant fetchedAt
) {
  private static final String SEP = "|";

  /** Redis 에 문자열로 저장하기 위한 직렬화. JSON 라이브러리에 의존하지 않는다. */
  public String serialize() {
    return String.join(
      SEP,
      str(price),
      str(change),
      str(changePercent),
      str(previousClose),
      str(high),
      str(low),
      str(open),
      String.valueOf(fetchedAt.toEpochMilli())
    );
  }

  public static Quote deserialize(String symbol, String raw) {
    String[] parts = raw.split("\\|", -1);
    if (parts.length != 8) {
      throw new IllegalArgumentException("손상된 시세 캐시: " + raw);
    }
    return new Quote(
      symbol,
      num(parts[0]),
      num(parts[1]),
      num(parts[2]),
      num(parts[3]),
      num(parts[4]),
      num(parts[5]),
      num(parts[6]),
      Instant.ofEpochMilli(Long.parseLong(parts[7]))
    );
  }

  private static String str(BigDecimal value) {
    return value == null ? "" : value.toPlainString();
  }

  private static BigDecimal num(String value) {
    return value.isEmpty() ? null : new BigDecimal(value);
  }
}
