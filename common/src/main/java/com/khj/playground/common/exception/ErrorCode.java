package com.khj.playground.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 오류 종류. 각 모듈이 자기 enum 으로 구현한다.
 * key 는 i18n/exception_{언어}.yml 의 최상위 키이고, 그 아래에 code(숫자 문자열)와 message 가 있다.
 */
public interface ErrorCode {
  String key();

  HttpStatus status();
}
