package com.khj.playground.auth.dto;

/** 회원가입 아이디 규칙. 영문/숫자/언더스코어 7~16자. */
public final class UsernamePolicy {

  public static final int MIN = 7;
  public static final int MAX = 16;
  public static final String REGEX = "^[a-zA-Z0-9_]+$";

  private UsernamePolicy() {}

  public static boolean isValid(String username) {
    return (
      username != null &&
      username.length() >= MIN &&
      username.length() <= MAX &&
      username.matches(REGEX)
    );
  }
}
