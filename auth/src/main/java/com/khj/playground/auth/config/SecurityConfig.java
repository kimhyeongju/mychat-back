package com.khj.playground.auth.config;

import com.khj.playground.auth.jwt.JwtAuthenticationFilter;
import com.khj.playground.common.exception.ApiErrorWriter;
import com.khj.playground.common.exception.CommonErrorCode;
import com.khj.playground.common.security.PublicEndpoints;
import com.khj.playground.common.security.TokenAuthenticationResolver;
import java.util.List;
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
 * /api/admin/** 는 ADMIN 권한만 접근할 수 있다.
 * 인증 없이 접근 가능한 경로: 회원가입/로그인/휴대폰 인증, 헬스체크, Swagger, WebSocket 핸드셰이크.
 * 그 외 API는 JwtAuthenticationFilter가 채워주는 인증 정보가 있어야 접근 가능.
 * 인증이 없거나 만료되면 401, 권한이 모자라면 403 이며 본문은 { code, message, status } 형식이다.
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

  private final TokenAuthenticationResolver tokenResolver;
  private final List<PublicEndpoints> moduleEndpoints;
  private final ApiErrorWriter errorWriter;

  public SecurityConfig(
    TokenAuthenticationResolver tokenResolver,
    List<PublicEndpoints> moduleEndpoints,
    ApiErrorWriter errorWriter
  ) {
    this.tokenResolver = tokenResolver;
    this.moduleEndpoints = moduleEndpoints;
    this.errorWriter = errorWriter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .anonymous(anonymous -> anonymous.disable())
      .sessionManagement(s ->
        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(auth -> {
        auth.requestMatchers(AUTH_PUBLIC).permitAll();

        // 각 모듈이 등록한 공개 경로를 메서드 단위로 허용한다.
        moduleEndpoints
          .stream()
          .flatMap(provider -> provider.endpoints().stream())
          .forEach(endpoint -> {
            if (endpoint.method() == null) {
              auth.requestMatchers(endpoint.pattern()).permitAll();
            } else {
              auth
                .requestMatchers(endpoint.method(), endpoint.pattern())
                .permitAll();
            }
          });

        // 운영자 전용 API. 권한은 JWT의 role 클레임(ROLE_ADMIN)으로 판단한다.
        auth.requestMatchers("/api/admin/**").hasRole("ADMIN");

        auth.anyRequest().authenticated();
      })
      // 인증 실패는 401, 권한 부족은 403 으로 나누고 본문에 언어별 메시지를 담는다.
      // 기본 동작은 둘 다 본문 없는 403 이라, 토큰이 만료돼도 프론트가 재발급을 시도하지 못했다.
      .exceptionHandling(e ->
        e
          .authenticationEntryPoint((request, response, ex) ->
            errorWriter.write(request, response, CommonErrorCode.UNAUTHORIZED)
          )
          .accessDeniedHandler((request, response, ex) ->
            errorWriter.write(request, response, CommonErrorCode.FORBIDDEN)
          )
      )
      .addFilterBefore(
        new JwtAuthenticationFilter(tokenResolver),
        UsernamePasswordAuthenticationFilter.class
      );
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
