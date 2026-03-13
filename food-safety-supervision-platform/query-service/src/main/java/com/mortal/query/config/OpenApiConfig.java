package com.mortal.query.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI queryServiceOpenApi() {
        return new OpenAPI().info(new Info()
            .title("query-service API")
            .description("预警统计接口文档。时间参数格式：yyyy-MM-dd'T'HH:mm:ss（UTC+8 本地时间）。")
            .version("v1"));
    }
}

