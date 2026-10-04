package com.khj.playground.auth.jwt;

import com.khj.playground.auth.enums.Role;
import com.khj.playground.common.security.TokenAuthenticationResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 토큰 → Authentication 변환을 담당한다.
 * HTTP 필터와 WebSocket 양쪽이 이 구현 하나만 사용하므로,
 * 인증 규칙이 한 곳에만 존재하게 된다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationResolverImpl
  implements TokenAuthenticationResolver {

  private static final String PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public Authentication resolve(String bearerHeader) {
    if (
      !StringUtils.hasText(bearerHeader) || !bearerHeader.startsWith(PREFIX)
    ) {
      return null;
    }

    String token = bearerHeader.substring(PREFIX.length());
    if (!jwtTokenProvider.validateToken(token)) {
      return null;
    }

    // role claim이 없는 토큰(Refresh Token)은 인증에 쓰지 않는다.
    Role role = jwtTokenProvider.getRole(token);
    if (role == null) {
      return null;
    }

    return new UsernamePasswordAuthenticationToken(
      jwtTokenProvider.getUsername(token),
      null,
      List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
    );
  }
}
