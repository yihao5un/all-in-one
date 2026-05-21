# Local Startup

Start infrastructure:

```bash
cd docker
docker compose -f docker-compose.infra.yml up -d
```

Publish Nacos configs after Nacos is ready:

```bash
cd ..
scripts/import-nacos-configs.sh
```

Then start backend services from `back-end`:

```bash
mvn -pl uno-auth -am spring-boot:run
mvn -pl uno-gateway -am spring-boot:run
```

If Docker containers are recreated from scratch, publish Nacos configs again before starting services.

> [!NOTE]
> 本项目的默认网关端口已由原先的 `8080` 调整为 `8088`，以避开本地环境（如 Python 简易服务器、Nginx 等）可能占用的 8080 端口冲突问题。
> 前端开发代理 (`front-end/vite.config.ts`)、AI 网关地址 (`ai/app/tools/hr_tools.py`)、本地 Nacos 网关备份配置 (`deploy/nacos/configs/uno-gateway`) 以及接口测试脚本 (`docs/api-test/`) 均已同步修改为 `8088`。

