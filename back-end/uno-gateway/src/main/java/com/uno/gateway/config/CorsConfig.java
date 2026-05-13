package com.uno.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 网关全局跨域配置
 * 解决浏览器预检请求 (OPTIONS) 及 Header 暴露问题
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // 允许携带 Cookie
        config.addAllowedOriginPattern("*"); // 允许所有来源
        config.addAllowedHeader("*"); // 允许所有 Header
        config.addAllowedMethod("*"); // 允许所有方法
        
        // ⚡️ 核心：暴露 Authorization 头，否则前端拿不到
        config.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
