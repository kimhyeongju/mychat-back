package com.khj.playground.auth.dto;

/**
 * 비밀번호 생성 규칙. 회원가입과 비밀번호 재설정이 같은 규칙을 쓰도록 한 곳에 둔다.
 * 프론트(src/utils/password.js)의 검사와 동일하게 유지해야 한다.
 */
public final class PasswordPolicy {

  /** 영문자·숫자·특수문자를 각각 1자 이상 포함하고, 공백 없이 8~64자. 한글 등 비ASCII 문자는 허용하지 않는다. */
  public static final String REGEX =
    "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9])[!-~]{8,64}$";

  public static final String MESSAGE =
    "비밀번호는 영문자, 숫자, 특수문자를 모두 포함해 8~64자로 입력해주세요.";

  private PasswordPolicy() {}
}
