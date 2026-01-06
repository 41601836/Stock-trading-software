package com.stock.analysis.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置类
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stockAnalysisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("股票分析系统 API")
                        .description("提供股票技术分析、筹码分析、大单监控、舆情分析、风险控制等功能的REST API")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}