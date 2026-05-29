package com.poker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("德州扑克游戏 API")
                        .version("1.0.0")
                        .description("德州扑克游戏后端API接口文档，包含认证、用户、房间管理等功能")
                        .contact(new Contact()
                                .name("Poker Game Team")));
    }
}