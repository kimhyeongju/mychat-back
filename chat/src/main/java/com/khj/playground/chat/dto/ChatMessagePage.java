package com.khj.playground.chat.dto;

import java.util.List;
import java.util.UUID;

/**
 * 커서 기반 페이지. content는 오래된 순으로 정렬되어 그대로 화면에 그릴 수 있다.
 * nextCursor로 더 과거 메시지를 요청한다.
 */
public record ChatMessagePage(
  List<ChatMessageResponse> content,
  UUID nextCursor,
  boolean hasMore
) {}
