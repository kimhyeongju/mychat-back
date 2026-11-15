package com.khj.playground.invest.service;

import com.khj.playground.invest.entity.SymbolAlias;
import com.khj.playground.invest.market.SymbolInfo;
import com.khj.playground.invest.repository.SymbolAliasRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한글 종목명 검색. 별칭 테이블 전체(수백 건)를 메모리에 올려두고 찾는다.
 * 외부 API 를 부르지 않으므로 호출 한도를 쓰지 않는다.
 *
 * 일치 방식: 공백 제거 후 부분 일치, 그리고 검색어가 초성만으로 되어 있으면 초성 일치(예: ㅇㅍ → 애플).
 */
@Component
@RequiredArgsConstructor
public class KoreanSymbolSearch {

  private static final Duration TTL = Duration.ofMinutes(10);
  private static final int MAX_RESULTS = 20;
  private static final char[] CHOSEONG = {
    'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
    'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
  };

  private final SymbolAliasRepository repository;

  private volatile Index index;

  /** 검색어에 한글(음절 또는 자모)이 하나라도 있으면 true. */
  public static boolean containsKorean(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if ((c >= '가' && c <= '힣') || (c >= 'ㄱ' && c <= 'ㅎ')) {
        return true;
      }
    }
    return false;
  }

  @Transactional(readOnly = true)
  public List<SymbolInfo> search(String rawQuery) {
    String query = normalize(rawQuery);
    if (query.isEmpty()) {
      return List.of();
    }
    boolean choseongOnly = isChoseongOnly(query);

    List<Hit> hits = new ArrayList<>();
    for (Entry entry : current().entries()) {
      int best = 0;
      for (Alias alias : entry.aliases()) {
        best = Math.max(best, score(alias, query, choseongOnly));
      }
      if (best > 0) {
        hits.add(new Hit(entry, best));
      }
    }
    hits.sort(
      Comparator
        .comparingInt(Hit::score)
        .reversed()
        .thenComparing(h -> h.entry().symbol())
    );
    return hits
      .stream()
      .limit(MAX_RESULTS)
      .map(h -> new SymbolInfo(h.entry().symbol(), h.entry().display(), "US"))
      .toList();
  }

  /** 별칭이 등록된 종목의 영문 이름. */
  @Transactional(readOnly = true)
  public Optional<String> findNameEn(String symbol) {
    Entry entry = current().bySymbol().get(symbol);
    return entry == null ? Optional.empty() : Optional.of(entry.nameEn());
  }

  private static int score(Alias alias, String query, boolean choseongOnly) {
    if (choseongOnly) {
      String c = alias.choseong();
      if (c.startsWith(query)) return 60;
      return c.contains(query) ? 30 : 0;
    }
    String name = alias.normalized();
    if (name.equals(query)) return 100;
    if (name.startsWith(query)) return 80;
    return name.contains(query) ? 40 : 0;
  }

  private Index current() {
    Index cached = index;
    if (cached != null && !cached.isExpired()) {
      return cached;
    }
    synchronized (this) {
      cached = index;
      if (cached == null || cached.isExpired()) {
        cached = load();
        index = cached;
      }
      return cached;
    }
  }

  private Index load() {
    // id 순서가 곧 대표 별칭 순서다.
    Map<String, List<SymbolAlias>> grouped = new LinkedHashMap<>();
    for (SymbolAlias a : repository.findAllByOrderByIdAsc()) {
      grouped.computeIfAbsent(a.getSymbol(), k -> new ArrayList<>()).add(a);
    }
    List<Entry> entries = new ArrayList<>();
    Map<String, Entry> bySymbol = new LinkedHashMap<>();
    grouped.forEach((symbol, rows) -> {
      String nameEn = rows.get(0).getNameEn();
      List<Alias> aliases = rows
        .stream()
        .map(r ->
          new Alias(normalize(r.getAlias()), choseong(normalize(r.getAlias())))
        )
        .toList();
      Entry entry = new Entry(
        symbol,
        rows.get(0).getAlias() + " (" + nameEn + ")",
        nameEn,
        aliases
      );
      entries.add(entry);
      bySymbol.put(symbol, entry);
    });
    return new Index(List.copyOf(entries), Map.copyOf(bySymbol), Instant.now());
  }

  /** 공백을 없애고 소문자로 맞춘다. "S&P 500" 과 "s&p500" 이 같아진다. */
  static String normalize(String text) {
    return text == null
      ? ""
      : text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
  }

  private static boolean isChoseongOnly(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c < 'ㄱ' || c > 'ㅎ') return false;
    }
    return true;
  }

  /** 한글 음절은 초성으로, 그 밖의 글자는 그대로 둔다. */
  static String choseong(String text) {
    StringBuilder sb = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= '가' && c <= '힣') {
        sb.append(CHOSEONG[(c - '가') / 588]);
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private record Alias(String normalized, String choseong) {}

  private record Entry(
    String symbol,
    String display,
    String nameEn,
    List<Alias> aliases
  ) {}

  private record Hit(Entry entry, int score) {}

  private record Index(
    List<Entry> entries,
    Map<String, Entry> bySymbol,
    Instant loadedAt
  ) {
    boolean isExpired() {
      return Duration.between(loadedAt, Instant.now()).compareTo(TTL) > 0;
    }
  }
}
