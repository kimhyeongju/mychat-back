package com.khj.playground.auth.service;

import com.khj.playground.auth.sms.SmsSender;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
  private static final int DAILY_LIMIT = 5;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final StringRedisTemplate redisTemplate;
  private final SmsSender smsSender;

  public void sendVerificationCode(String phoneNumber) {
    checkCooldown(phoneNumber);
    checkDailyLimit(phoneNumber);

    String code = generateCode();
    redisTemplate
      .opsForValue()
      .set(CODE_KEY_PREFIX + phoneNumber, code, CODE_TTL);
    smsSender.send(
      phoneNumber,
      "[khj-playground] 인증번호는 " + code + " 입니다."
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
    return String.format("%06d", RANDOM.nextInt(1_000_000));
  }

  /** 같은 번호로 1분 안에 재요청하면 막는다. */
  private void checkCooldown(String phoneNumber) {
    String key = "sms:cooldown:" + phoneNumber;
    Boolean allowed = redisTemplate
      .opsForValue()
      .setIfAbsent(key, "1", Duration.ofMinutes(1));

    if (!Boolean.TRUE.equals(allowed)) {
      throw new ResponseStatusException(
        HttpStatus.TOO_MANY_REQUESTS,
        "1분 후에 다시 시도해주세요."
      );
    }
  }

  /**
   * 번호당 하루 요청 횟수를 제한한다.
   * 솔라피 개인 계정은 일일 발송 한도가 있어, 남용되면 정상 가입까지 막힌다.
   */
  private void checkDailyLimit(String phoneNumber) {
    String key = "sms:daily:" + phoneNumber;
    Long count = redisTemplate.opsForValue().increment(key);

    if (count != null && count == 1L) {
      // 첫 요청일 때만 만료를 건다. 이후 증가에는 TTL이 그대로 유지된다.
      redisTemplate.expire(key, Duration.ofDays(1));
    }

    if (count != null && count > DAILY_LIMIT) {
      throw new ResponseStatusException(
        HttpStatus.TOO_MANY_REQUESTS,
        "하루 인증 요청 횟수를 초과했습니다. 내일 다시 시도해주세요."
      );
    }
  }
}
