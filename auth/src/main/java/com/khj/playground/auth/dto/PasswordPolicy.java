package com.khj.playground.auth.dto;

/**
 * 비밀번호 생성 규칙. 회원가입과 비밀번호 재설정이 같은 규칙을 쓰도록 한 곳에 둔다.
 * 프론트(src/utils/password.js)의 검사와 동일하게 유지해야 한다.
 * 사용자에게 보이는 문구는 auth 모듈의 i18n/valid-message_*.yml 의 user-dto.password.pattern 에 있다.
 */
public final class PasswordPolicy {

  /** 영문자·숫자·특수문자를 각각 1자 이상 포함하고, 공백 없이 8~64자. 한글 등 비ASCII 문자는 허용하지 않는다. */
  public static final String REGEX =
    "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9])[!-~]{8,64}$";

  public static final String MESSAGE = "{user-dto.password.pattern}";

  private PasswordPolicy() {}
}
