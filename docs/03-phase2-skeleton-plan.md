# Phase 2: 微服务核心骨架搭建计划 (Core Microservice Skeleton)

## 目标与愿景
构建一个稳固、规范、符合大厂开发标准的微服务父工程体系，通过提取公共组件，确保所有子服务在异常处理、返回值封装、依赖版本上保持绝对的一致性。全面拥抱 Java 21 和 Spring Boot 3 生态。

## 架构核心动作分解

### 1. 统一依赖管理 (Parent POM) ✅ [已完成]
- **大厂黄金三角**：Spring Boot 3.2.4 + Spring Cloud 2023.0.1 + Spring Cloud Alibaba 2023.0.1.0。
- **现代化特性**：引入 Java 21 编译支持，彻底解决 `lombok` + `mapstruct` 在高版本 JDK 下的注解处理器冲突问题。
- **规范迁移**：使用 `mybatis-plus-spring-boot3-starter` 适配 Jakarta EE 规范。

### 2. 抽取微服务公共核心模块 (`uno-common`)
> “不要重复造轮子” —— 所有微服务强依赖的底层基石。

- **`Result<T>` 统一响应泛型类**：规范化前端交互的 JSON 格式（包含 code、message、data）。
- **`ResultCodeEnum` 状态码枚举**：消除代码中的魔法数字（Magic Number），统一管理业务状态码。
- **`GlobalExceptionHandler` 全局异常处理器**：通过 `@ControllerAdvice` 统一捕获异常，防止系统抛出堆栈报错给客户端，保障服务稳定性和安全性。
- **`UnoException` 业务异常基类**：用于业务逻辑中手动抛出自定义状态，支持平滑向下兼容。

### 3. 构建微服务网关入口 (`uno-gateway`) [待执行]
- 引入 Spring Cloud Gateway。
- 整合 Nacos Discovery 实现动态路由。
- 搭建基础鉴权过滤器骨架。

---
**当前进度**：正在全自动并行生成 `uno-common` 模块的代码和配置...
