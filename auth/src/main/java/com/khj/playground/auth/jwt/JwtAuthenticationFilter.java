package com.khj.playground.auth.jwt;

import com.khj.playground.common.security.TokenAuthenticationResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER = "Authorization";

  private final TokenAuthenticationResolver tokenResolver;

  public JwtAuthenticationFilter(TokenAuthenticationResolver tokenResolver) {
    this.tokenResolver = tokenResolver;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    Authentication authentication = tokenResolver.resolve(
      request.getHeader(HEADER)
    );

    if (authentication != null) {
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }
}
