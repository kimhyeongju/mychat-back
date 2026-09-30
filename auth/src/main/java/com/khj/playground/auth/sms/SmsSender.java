package com.khj.playground.auth.sms;

/**
 * 문자 발송 추상화.
 * 업체를 바꾸거나 프로필별로 mock을 쓰기 위해 인터페이스를 둔다.
 */
public interface SmsSender {
  void send(String phoneNumber, String message);
}
