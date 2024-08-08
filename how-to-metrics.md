# Instrumentação de Metrics

## passo 1
Inclua as dependências no arquivo `pom.xml`:

```xml
<!-- built-in endpoints, como: /metrics e /health -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- micrometer para disponibilizar métrics no formato do prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

## passo 2
Exponha a rota do prometheus (e outras rotas) via arquivo `application.yml` / `application.properties` ou variável de ambiente. Ex via `application.yml`:

```yaml
# expondo rotas: /actuator/<health, info, metrics, prometheus>
management.endpoints.web.exposure.include: health,info,metrics,prometheus
```

## passo 3
Execute a aplicação e valide se rota `/actuator/prometheus` está acessível


## passo 4
Acesse o prometheus (localmente: `http://localhost:9090`) e execute as queries

```prometheus
# todas requisições com status 4xx e 5xx
http_server_requests_seconds_count{status =~ '4.+'}
http_server_requests_seconds_count{status =~ '5.+'}

# todas as requisições, exceto /actuator/prometheus
http_server_requests_seconds_count{uri !~ ".+prometheus"}

# múltiplos filtros: url exceto prometheus e status 2xx
http_server_requests_seconds_count{uri !~ ".+prometheus", status =~ "2.+"}

# múltiplos filtros: url exceto prometheus/metrics/health e status 2xx
http_server_requests_seconds_count{uri !~ ".+prometheus|.+metrics|.+health", status =~ "2.+"}

# requisições 2xx em relação ao tempo (resolução)
http_server_requests_seconds_sum{uri !~ ".+prometheus|.+metrics|.+health", status =~ "2.+"}
```


## docs
* https://github.com/docker/awesome-compose/tree/master/prometheus-grafana
* https://prometheus.io/docs/tutorials/understanding_metric_types/
