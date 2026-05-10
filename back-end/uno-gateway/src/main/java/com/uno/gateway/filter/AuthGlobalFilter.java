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

import java.util.List;

/**
 * 网关全局统一鉴权过滤器
 * 拦截所有请求，校验 JWT Token，实现微服务群的统一安全管控
 */
@Component
@Slf4j
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 白名单路径（无需鉴权即可访问）
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 判断是否在白名单内
        for (String whitePath : WHITE_LIST) {
            if (path.contains(whitePath)) {
                log.info("【网关放行】白名单请求: {}", path);
                return chain.filter(exchange);
            }
        }

        // 2. 提取 Header 中的 Token
        String token = request.getHeaders().getFirst("Authorization");

        // 3. 校验 Token 存在性
        if (StrUtil.isBlank(token)) {
            log.warn("【网关拦截】非法请求，未携带凭证: {}", path);
            return unauthorizedResponse(exchange);
        }

        try {
            // 解析真实的 JWT Token
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            io.jsonwebtoken.Claims claims = com.uno.common.utils.JwtUtils.parseToken(token);
            String userId = claims.getSubject();
            
            // 将 userId 塞入后续的 HTTP 请求头中，做到下游微服务“无感知”获取当前操作用户
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .build();
            exchange = exchange.mutate().request(mutatedRequest).build();
            
            log.info("【网关放行】鉴权通过, UserId: {}", userId);
        } catch (Exception e) {
            log.error("【网关拦截】Token 解析失败或已过期: {}", e.getMessage());
            return unauthorizedResponse(exchange);
        }

        return chain.filter(exchange);
    }

    /**
     * 响应 401 未授权异常
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // 这里为了演示简便直接返回 HTTP 状态码，后续可以扩展封装统一的 JSON 返回
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        // 过滤器执行优先级，越小越靠前
        return -100;
    }
}
