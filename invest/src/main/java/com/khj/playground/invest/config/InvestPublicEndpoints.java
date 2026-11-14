package com.khj.playground.invest.config;

import com.khj.playground.common.security.PublicEndpoint;
import com.khj.playground.common.security.PublicEndpoints;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 시세·검색·랭킹·환율은 비로그인도 볼 수 있다.
 * 계좌·포트폴리오·거래는 등록하지 않으므로 자동으로 인증이 필요해진다.
 */
@Component
public class InvestPublicEndpoints implements PublicEndpoints {

  @Override
  public List<PublicEndpoint> endpoints() {
    return List.of(
      PublicEndpoint.get("/api/invest/symbols/search"),
      PublicEndpoint.get("/api/invest/symbols/*"),
      PublicEndpoint.get("/api/invest/ranking"),
      PublicEndpoint.get("/api/invest/fx")
    );
  }
}
