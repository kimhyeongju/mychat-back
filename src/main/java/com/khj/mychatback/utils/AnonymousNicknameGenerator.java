package com.khj.mychatback.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * "익명카치", "익명여우" 같은 형태의 랜덤 닉네임을 생성한다.
 * 방 내부에서 중복되면 뒤에 두 자리 숫자를 붙여 구분한다 (서비스 레이어에서 처리).
 */
public final class AnonymousNicknameGenerator {

  private static final String[] ANIMALS = {
    "카치",
    "여우",
    "고양이",
    "너구리",
    "수달",
    "부엉이",
    "다람쥐",
    "고슴도치",
    "펭귄",
    "코알라",
    "북극곰",
    "사슴",
    "토끼",
    "햄스터",
    "강아지",
    "고릴라",
    "코끼리",
    "사자",
    "호랑이",
    "기린",
    "코뿔소",
    "하마",
    "악어",
    "캥거루",
  };

  private AnonymousNicknameGenerator() {}

  public static String generate() {
    String animal =
      ANIMALS[ThreadLocalRandom.current().nextInt(ANIMALS.length)];
    return "익명" + animal;
  }

  public static String generateWithSuffix(int suffix) {
    return generate() + suffix;
  }
}
