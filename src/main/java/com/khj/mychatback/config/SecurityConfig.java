package com.khj.mychatback.config;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.khj.mychatback.config.jwt.JwtAuthenticationFilter;
import com.khj.mychatback.config.jwt.JwtTokenProvider;

/**
 * 인증 없이 접근 가능한 경로: 회원가입/로그인/휴대폰 인증, 헬스체크, Swagger, WebSocket 핸드셰이크.
 * 그 외 API는 JwtAuthenticationFilter가 채워주는 인증 정보가 있어야 접근 가능.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String[] PUBLIC_ENDPOINTS = {
    "/api/auth/phone/**",
    "/api/auth/signup",
    "/api/auth/login",
    "/api/auth/reissue",
    "/api/auth/find-id",
    "/api/auth/reset-password",
    "/api/hello",
    "/api/chat/rooms/nearby",
    "/api/chat/rooms/location",
    "/api/chat/rooms/*/join",
    "/api/chat/rooms/*/messages",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/ws/**",
    "/actuator/health",
  };

  private final JwtTokenProvider jwtTokenProvider;

  public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .anonymous(anonymous -> anonymous.disable())
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
          .permitAll()
          .requestMatchers(PUBLIC_ENDPOINTS)
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

  /**
   * 로컬 개발용 CORS 허용 목록.
   * 같은 와이파이의 스마트폰에서 PC의 LAN IP(예: 192.168.x.x:5173)로 접속하는 것도 허용하기 위해
   * localhost뿐 아니라 사설 IP 대역 패턴도 함께 허용한다.
   * TODO: prod 프로필에서는 실제 프론트엔드 도메인(https://...)만 허용하도록 분리 필요.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(
      List.of(
        "http://localhost",
        "http://192.168.*.*",
        "http://localhost:5173",
        "http://192.168.*.*:5173",
        "http://10.*.*.*:5173",
        "https://*.ngrok-free.app",
        "https://*.ngrok.io"
      )
    );
    configuration.setAllowedMethods(
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    );
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
