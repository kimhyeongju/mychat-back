package com.khj.playground.common.exception;

import java.util.List;

/**
 * 오류 응답 본문. 프론트는 message 를 그대로 사용자에게 보여준다.
 *
 * @param code 오류 코드(문자열). exception_*.yml 의 code
 * @param fieldErrors 입력 검증 실패일 때 필드별 사유. 없으면 빈 목록
 */
public record ErrorBody(
  String code,
  String message,
  int status,
  List<FieldIssue> fieldErrors
) {
  public record FieldIssue(String field, String message) {}
}
