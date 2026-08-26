package com.khj.mychatback.api.auth.service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 인증번호는 DB가 아닌 Redis에 TTL과 함께 저장한다 (휘발성 데이터이므로 자동 만료가 필요).
 * - phone:code:{phoneNumber}     → 발급된 6자리 인증번호, 5분 후 자동 삭제
 * - phone:verified:{phoneNumber} → 인증 성공 여부, 30분간 유지 (그 사이에 회원가입을 완료해야 함)
 */
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

  private static final Duration CODE_TTL = Duration.ofMinutes(5);
  private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
  private static final String CODE_KEY_PREFIX = "phone:code:";
  private static final String VERIFIED_KEY_PREFIX = "phone:verified:";

  private final StringRedisTemplate redisTemplate;
  private final SmsService smsService;

  public void sendVerificationCode(String phoneNumber) {
    String code = generateCode();
    redisTemplate
      .opsForValue()
      .set(CODE_KEY_PREFIX + phoneNumber, code, CODE_TTL);
    smsService.send(
      phoneNumber,
      "[mychat] 인증번호는 [" + code + "] 입니다. 5분 이내에 입력해주세요."
    );
  }

  /**
   * @return 인증 성공 여부. 성공 시 코드는 즉시 삭제하고 "인증 완료" 상태를 별도로 저장한다.
   */
  public boolean verifyCode(String phoneNumber, String code) {
    String savedCode = redisTemplate
      .opsForValue()
      .get(CODE_KEY_PREFIX + phoneNumber);

    if (savedCode == null || !savedCode.equals(code)) {
      return false;
    }

    redisTemplate.delete(CODE_KEY_PREFIX + phoneNumber);
    redisTemplate
      .opsForValue()
      .set(VERIFIED_KEY_PREFIX + phoneNumber, "true", VERIFIED_TTL);
    return true;
  }

  /** 회원가입 시점에 "이 번호가 방금 인증을 마쳤는지" 확인하는 용도 */
  public boolean isVerified(String phoneNumber) {
    return "true".equals(
        redisTemplate.opsForValue().get(VERIFIED_KEY_PREFIX + phoneNumber)
      );
  }

  public void clearVerified(String phoneNumber) {
    redisTemplate.delete(VERIFIED_KEY_PREFIX + phoneNumber);
  }

  private String generateCode() {
    int code = ThreadLocalRandom.current().nextInt(0, 1_000_000);
    return String.format("%06d", code);
  }
}
