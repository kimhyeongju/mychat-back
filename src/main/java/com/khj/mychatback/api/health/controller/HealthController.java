package com.khj.mychatback.api.health.controller;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배포 파이프라인/서버 동작 확인용 임시 엔드포인트.
 * 이후 실제 기능 API로 대체되기 전까지 "서비스가 정상적으로 떠 있는지" 확인하는 용도.
 */
@RestController
public class HealthController {

  @GetMapping("/api/hello")
  public Map<String, Object> hello() {
    return Map.of(
      "message",
      "Hello, my web is running!",
      "timestamp",
      LocalDateTime.now()
    );
  }
}
