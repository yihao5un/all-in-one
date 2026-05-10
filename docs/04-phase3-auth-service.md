# Phase 3: 认证中心服务 (uno-auth) 建设

## 目标与定位
微服务架构下的统一看门人（Gatekeeper）。负责用户凭证校验、发放 JWT 令牌（Token），并与网关 (`uno-gateway`) 的 `AuthGlobalFilter` 形成闭环。

## 核心技术栈
- **Web**: Spring MVC (提供 `/auth/login` 接口)
- **Token**: jjwt 0.12.x (安全规范更高的新一代 JWT 处理库)
- **Database**: MyBatis-Plus + MySQL (为后续真实用户表的 CRUD 做准备)
- **Discovery/Config**: Nacos 客户端

## 架构连通性测试 (End-to-End Test)
我们将实现一个最小原型（MVP），硬编码一组账号密码：
1. 客户端访问 `http://localhost:8080/uno-auth/auth/login`
2. 经过网关 (`uno-gateway`)，白名单放行，基于 Nacos 自动路由到 `uno-auth` 节点。
3. `uno-auth` 验证通过后，调用 `uno-common` 的 `JwtUtils` 生成 Token。
4. 客户端拿到 Token，访问其他受保护资源测试网关拦截。
