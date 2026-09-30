package com.khj.playground.auth.sms;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 운영 환경용. 솔라피 API로 실제 문자를 보낸다.
 * 솔라피는 HMAC-SHA256 서명 인증을 쓰므로 공식 SDK에 맡긴다.
 */
@Slf4j
@Component
@Profile("prod")
public class SolapiSmsSender implements SmsSender {

  private final DefaultMessageService messageService;
  private final String senderNumber;

  public SolapiSmsSender(
    @Value("${sms.solapi.api-key}") String apiKey,
    @Value("${sms.solapi.api-secret}") String apiSecret,
    @Value("${sms.solapi.sender}") String senderNumber
  ) {
    this.messageService =
      SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
    // 솔라피 콘솔에 사전 등록된 번호만 발신번호로 쓸 수 있다.
    this.senderNumber = senderNumber;
  }

  @Override
  public void send(String phoneNumber, String message) {
    Message sms = new Message();
    // 발신/수신번호는 반드시 01012345678 형식이어야 한다. 하이픈이나 + 기호는 넣을 수 없다.
    sms.setFrom(senderNumber);
    sms.setTo(phoneNumber);
    sms.setText(message);

    try {
      messageService.send(sms);
    } catch (SolapiMessageNotReceivedException e) {
      log.error(
        "솔라피 문자 발송 실패: to={}, failed={}",
        phoneNumber,
        e.getFailedMessageList(),
        e
      );
      throw toClientError();
    } catch (Exception e) {
      log.error("솔라피 문자 발송 중 오류: to={}", phoneNumber, e);
      throw toClientError();
    }
  }

  /** 실패 원인(잔액 부족, 발신번호 미등록 등)은 로그에만 남기고 사용자에게는 일반 메시지를 준다. */
  private ResponseStatusException toClientError() {
    return new ResponseStatusException(
      HttpStatus.SERVICE_UNAVAILABLE,
      "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요."
    );
  }
}
