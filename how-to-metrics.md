# Instrumentação de Metrics

docs
* https://github.com/docker/awesome-compose/tree/master/prometheus-grafana
* https://github.com/aha-oretama/spring-boot-prometheus-grafana-sample/blob/master/docker-compose.yml
* https://www.tutorialworks.com/spring-boot-prometheus-micrometer/
* https://medium.com/@aleksanderkolata/spring-boot-micrometer-prometheus-and-grafana-how-to-add-custom-metrics-to-your-application-712c6f895f6b

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

