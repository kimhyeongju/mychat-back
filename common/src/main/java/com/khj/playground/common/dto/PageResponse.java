package com.khj.playground.common.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Spring의 Page를 그대로 직렬화하면 내부 구조가 노출되고 형태도 불안정하다.
 * 프론트가 필요한 것만 담아 안정적인 계약을 만든다.
 */
public record PageResponse<T>(
  List<T> content,
  int page,
  int size,
  long totalElements,
  int totalPages,
  boolean first,
  boolean last
) {
  public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
    return new PageResponse<>(
      page.getContent().stream().map(mapper).toList(),
      page.getNumber(),
      page.getSize(),
      page.getTotalElements(),
      page.getTotalPages(),
      page.isFirst(),
      page.isLast()
    );
  }
}
