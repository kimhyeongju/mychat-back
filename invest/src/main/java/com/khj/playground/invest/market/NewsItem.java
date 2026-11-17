package com.khj.playground.invest.market;

import java.time.Instant;

/** 제공자가 준 뉴스 한 건. summary, source, url, createdAt 은 없을 수 있다. */
public record NewsItem(
  long id,
  String headline,
  String summary,
  String source,
  String url,
  Instant createdAt
) {}
