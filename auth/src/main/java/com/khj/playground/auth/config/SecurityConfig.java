package com.khj.playground.auth.config;

import com.khj.playground.auth.jwt.JwtAuthenticationFilter;
import com.khj.playground.auth.jwt.JwtTokenProvider;
import com.khj.playground.common.security.PublicEndpoints;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 인증 없이 접근 가능한 경로: 회원가입/로그인/휴대폰 인증, 헬스체크, Swagger, WebSocket 핸드셰이크.
 * 그 외 API는 JwtAuthenticationFilter가 채워주는 인증 정보가 있어야 접근 가능.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String[] AUTH_PUBLIC = {
    "/api/auth/phone/**",
    "/api/auth/signup",
    "/api/auth/check-username",
    "/api/auth/check-nickname",
    "/api/auth/login",
    "/api/auth/reissue",
    "/api/auth/find-id",
    "/api/auth/reset-password",
    "/api/hello",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/actuator/health",
  };

  private final JwtTokenProvider jwtTokenProvider;
  private final List<PublicEndpoints> moduleEndpoints;

  public SecurityConfig(
    JwtTokenProvider jwtTokenProvider,
    List<PublicEndpoints> moduleEndpoints
  ) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.moduleEndpoints = moduleEndpoints;
  }

  private String[] allPublicPatterns() {
    return Stream
      .concat(
        Arrays.stream(AUTH_PUBLIC),
        moduleEndpoints.stream().flatMap(e -> Arrays.stream(e.patterns()))
      )
      .toArray(String[]::new);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .anonymous(anonymous -> anonymous.disable())
      .sessionManagement(s ->
        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers(allPublicPatterns())
          .permitAll()
          .anyRequest()
          .authenticated()
      )
      .addFilterBefore(
        new JwtAuthenticationFilter(jwtTokenProvider),
        UsernamePasswordAuthenticationFilter.class
      );
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
