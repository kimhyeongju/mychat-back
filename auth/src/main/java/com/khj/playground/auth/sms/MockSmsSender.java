package com.khj.playground.auth.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local, dev 환경용. 실제 발송 없이 로그로만 남긴다.
 * 개발 중에 문자 비용이 나가거나 하루 발송 한도를 소진하는 것을 막는다.
 */
@Slf4j
@Component
@Profile("!prod")
public class MockSmsSender implements SmsSender {

  @Override
  public void send(String phoneNumber, String message) {
    log.info("[SMS-MOCK] to={}, message={}", phoneNumber, message);
  }
}
