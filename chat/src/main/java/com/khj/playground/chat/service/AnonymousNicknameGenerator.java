package com.khj.playground.chat.service;

import java.security.SecureRandom;
import java.util.List;

/** 익명 사용자에게 부여할 랜덤 닉네임을 만든다. */
public final class AnonymousNicknameGenerator {

  private static final List<String> ADJECTIVES = List.of(
    "졸린",
    "배고픈",
    "신난",
    "느긋한",
    "수줍은",
    "용감한",
    "엉뚱한",
    "까칠한",
    "다정한",
    "성실한",
    "엉성한",
    "날쌘",
    "포근한",
    "심심한"
  );

  private static final List<String> ANIMALS = List.of(
    "수달",
    "너구리",
    "고슴도치",
    "부엉이",
    "펭귄",
    "다람쥐",
    "두더지",
    "해달",
    "쿼카",
    "알파카",
    "미어캣",
    "라쿤",
    "비버",
    "오소리"
  );

  private static final SecureRandom RANDOM = new SecureRandom();

  private AnonymousNicknameGenerator() {}

  public static String generate() {
    String adjective = ADJECTIVES.get(RANDOM.nextInt(ADJECTIVES.size()));
    String animal = ANIMALS.get(RANDOM.nextInt(ANIMALS.size()));
    // 조합이 196가지뿐이라 숫자를 붙여 충돌 가능성을 낮춘다.
    return adjective + animal + RANDOM.nextInt(100);
  }
}
