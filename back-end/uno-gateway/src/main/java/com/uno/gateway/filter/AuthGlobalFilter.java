package com.uno.gateway.filter;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import io.jsonwebtoken.Claims;
import com.uno.common.utils.JwtUtils;

import java.util.List;

@Component
@Slf4j
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = List.of("/auth/login", "/auth/register");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单放行
        for (String whitePath : WHITE_LIST) {
            if (path.contains(whitePath)) {
                return chain.filter(exchange);
            }
        }

        // 2. 提取 Token (兼容 Header 和 URL 参数，方便直接跳转调试)
        String token = request.getHeaders().getFirst("Authorization");
        if (StrUtil.isBlank(token)) {
            token = request.getHeaders().getFirst("token");
        }
        if (StrUtil.isBlank(token)) {
            token = request.getQueryParams().getFirst("token");
        }

        if (StrUtil.isBlank(token)) {
            log.warn("【网关拦截】缺少 Token: {}", path);
            return unauthorizedResponse(exchange);
        }

        try {
            // 兼容 Bearer 前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 解析 Token
            Claims claims = JwtUtils.parseToken(token);
            String userId = claims.getSubject();
            
            // 传递用户信息给下游
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.error("【网关拦截】Token 无效: {}", e.getMessage());
            return unauthorizedResponse(exchange);
        }
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
