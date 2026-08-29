package com.khj.mychatback.api.user.controller;

import com.khj.mychatback.api.user.dto.UserSummaryResponse;
import com.khj.mychatback.api.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /** SecurityConfig의 PUBLIC_ENDPOINTS에 없으므로 인증(JWT)이 반드시 필요하다. */
  @GetMapping("/search")
  public List<UserSummaryResponse> search(
    @RequestParam("keyword") String keyword,
    Authentication authentication
  ) {
    return userService.searchByNickname(keyword, authentication.getName());
  }
}
