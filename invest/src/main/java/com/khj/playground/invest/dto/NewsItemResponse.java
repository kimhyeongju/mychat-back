package com.khj.playground.invest.dto;

import java.time.Instant;

/** 화면용 뉴스. 요약은 HTML 을 걷어낸 일반 텍스트이고, url 은 http(s) 만 허용한다. */
public record NewsItemResponse(
  long id,
  String headline,
  String summary,
  String source,
  String url,
  Instant createdAt
) {}
