package com.khj.mychatback.api.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * 알리고(https://smartsms.aligo.in) SMS 발송 연동.
 * sms.aligo.mock=true 인 환경(local/test/dev)에서는 실제 발송 없이 로그만 남긴다.
 * 운영(prod)에서는 mock=false + 실제 발급받은 API 키를 환경변수로 주입해야 한다.
 */
@Slf4j
@Service
public class AligoSmsService implements SmsService {

    private static final String ALIGO_SEND_URL = "https://apis.aligo.in/send/";

    private final RestClient restClient;
    private final String apiKey;
    private final String userId;
    private final String sender;
    private final boolean mock;

    public AligoSmsService(
        @Value("${sms.aligo.api-key}") String apiKey,
        @Value("${sms.aligo.user-id}") String userId,
        @Value("${sms.aligo.sender}") String sender,
        @Value("${sms.aligo.mock:true}") boolean mock
    ) {
        this.restClient = RestClient.builder().build();
        this.apiKey = apiKey;
        this.userId = userId;
        this.sender = sender;
        this.mock = mock;
    }

    @Override
    public void send(String phoneNumber, String message) {
        if (mock) {
            log.info("[SMS-MOCK] to={}, message={}", phoneNumber, message);
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("key", apiKey);
        form.add("user_id", userId);
        form.add("sender", sender);
        form.add("receiver", phoneNumber);
        form.add("msg", message);

        String response = restClient.post()
                .uri(ALIGO_SEND_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        log.info("Aligo SMS response: {}", response);
    }
}
