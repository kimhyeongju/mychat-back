package com.khj.playground.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private static final String BEARER = "bearerAuth";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
      .info(
        new Info()
          .title("khj-playground API")
          .description("채팅 / 게시판 / 모의투자 서비스의 공용 백엔드")
          .version("v1")
      )
      // 우측 상단 Authorize 버튼에 토큰을 넣으면 모든 요청에 자동으로 실린다.
      .addSecurityItem(new SecurityRequirement().addList(BEARER))
      .components(
        new Components()
          .addSecuritySchemes(
            BEARER,
            new SecurityScheme()
              .type(SecurityScheme.Type.HTTP)
              .scheme("bearer")
              .bearerFormat("JWT")
          )
      );
  }
}
