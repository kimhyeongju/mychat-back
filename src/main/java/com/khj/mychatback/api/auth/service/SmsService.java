package com.khj.mychatback.api.auth.service;

public interface SmsService {

    /**
     * @param phoneNumber 수신 번호 (하이픈 없이 숫자만, 예: 01012345678)
     * @param message     발송 메시지 본문
     */
    void send(String phoneNumber, String message);
}
