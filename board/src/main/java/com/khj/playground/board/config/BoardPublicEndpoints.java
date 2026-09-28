package com.khj.playground.board.config;

import com.khj.playground.common.security.PublicEndpoint;
import com.khj.playground.common.security.PublicEndpoints;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 게시판은 읽기만 공개한다.
 * POST/PATCH/DELETE는 등록하지 않으므로 자동으로 인증이 필요해진다.
 */
@Component
public class BoardPublicEndpoints implements PublicEndpoints {

  @Override
  public List<PublicEndpoint> endpoints() {
    return List.of(
      PublicEndpoint.get("/api/boards"),
      PublicEndpoint.get("/api/boards/*"),
      PublicEndpoint.get("/api/boards/*/posts"),
      PublicEndpoint.get("/api/boards/*/posts/*"),
      PublicEndpoint.get("/api/posts/*/comments"),
      PublicEndpoint.get("/api/posts/*/attachments"),
      PublicEndpoint.get("/api/attachments/*")
    );
  }
}
