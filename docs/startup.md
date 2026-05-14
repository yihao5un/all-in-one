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
