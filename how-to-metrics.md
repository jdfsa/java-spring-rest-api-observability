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
Acesse a rota `/actuator` e confira se as rotas `/actuator/metrics` e `/actuator/prometheus` estão habilitadas. Se estiverem, elas aparecerão na lista, conforme exemplo:

```json
{
    "_links": {
        "self": {
            "href": "http://localhost:8080/actuator",
            "templated": false
        },
        "health-path": {
            "href": "http://localhost:8080/actuator/health/{*path}",
            "templated": true
        },
        "health": {
            "href": "http://localhost:8080/actuator/health",
            "templated": false
        },
        "info": {
            "href": "http://localhost:8080/actuator/info",
            "templated": false
        },
        "prometheus": {
            "href": "http://localhost:8080/actuator/prometheus",
            "templated": false
        },
        "metrics-requiredMetricName": {
            "href": "http://localhost:8080/actuator/metrics/{requiredMetricName}",
            "templated": true
        },
        "metrics": {
            "href": "http://localhost:8080/actuator/metrics",
            "templated": false
        }
    }
}
```

Você pode acessar as rotas diretamente, ex:

> http://localhost:8080/actuator/metrics

> http://localhost:8080/actuator/prometheus



## passo 4 - /metrics

É possível realizar queries sobre as métricas através da rota `/atuator/metrics`.

> Query: listando conexões do Hikari
> http://localhost:28080/actuator/metrics/hikaricp.connections

Query: filtro de JVM memory por `area = heap`
> http://localhost:8080/actuator/metrics/jvm.memory.max?tag=area:heap

Query: filtro de JVM memory por `id = heap`
> http://localhost:8080/actuator/metrics/jvm.memory.max?tag=area:heap

Query: filtro combinado de JVM memory por `area = heap` e `id = G1 Old Gen`
> http://localhost:8080/actuator/metrics/jvm.memory.max?tag=area:heap&tag=id:G1+Old+Gen



## passo 5 - /prometheus

Em vez de ter que realizar queries uma a uma, o micrometer já expõe essas métricas em uma única consulta, no formato compatível com o prometheus.

Configure o serviço do Prometheus para capturar as métricas expostas pela API:

```yaml
scrape_configs:
  - job_name: 'StoreAppMetrics'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 3s
    static_configs:
      - targets: ['host.docker.internal:8080']
        labels:
          application: 'store-api'
```

Após executar o prometheus, acesese o prometheus (ex: localmente `http://localhost:9090`) e execute algumas queries:

```shell
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


# Plus: Prometheus com push Gateway

Nem sempre é possível expor um endpoint que seja acessível pelo Prometheus. Por exemplo, você pode ter aplicações batch ou workers que são estimulados de forma ad-hoc, agendados ou a partir de eventos de fila, que podem executar em infraestrutura mais tradicional, como servidores/VMs Linux e Windows.

Nesse caso você precisa instrumentar a aplicação para enviar proativamente as métricas ao Prometheus através do recurso chamado Push Gateway.

## passo 1 - infra docker
Inclua o serviço do `pushgateway` no `docker-compose.yml`. Ele geralmente expõe a porta `9091` para integração.

```yaml
services:
  # ....
  # outros serviços
  # ....
  pushgateway:
    image: prom/pushgateway
    container_name: pushgateway
    restart: unless-stopped
    expose:
      - 9091
    ports:
      - "9091:9091"
    labels:
    org.label-schema.group: "monitoring"
```

## passo 2 - instrumentação da aplicação
Além das dependências já adicionadas, inclua no `pom.xml` a lib `io.prometheus.simpleclient_pushgateway`:

```xml
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>simpleclient_pushgateway</artifactId>
</dependency>
```

No arquivo `src/main/resources/application.yml`, configure a integração com o pushgateway:

```yaml
spring.application.name: store-api
server.port: 28080
management:
  endpoints.web.exposure.include: health,info,metrics,prometheus
  metrics:
    tags:
      application: store api
    export:
      prometheus:
        pushgateway:
          enabled: true
          base-url: http://localhost:9091
          shutdown-operation: push
```

Adicione a classe de configuração (`@Configuration`) para configurar o componente de integração com o pushgateway:

```java
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

@Configuration
public class PushGatewayConfiguration {

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(@Value("${spring.profiles.active:default}") String activeEnvProfile) {
        return registry -> registry.config()
                .commonTags(
                        "env", activeEnvProfile
                );
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    @Primary
    public PrometheusPushGatewayManager prometheusPushGatewayManager(CollectorRegistry collectorRegistry,
                                                                     PrometheusProperties prometheusProperties,
                                                                     Environment environment) throws MalformedURLException {
        PrometheusProperties.Pushgateway properties = prometheusProperties.getPushgateway();
        Duration pushRate = properties.getPushRate();
        String job = appName;
        Map<String, String> groupingKey = properties.getGroupingKey();
        PrometheusPushGatewayManager.ShutdownOperation shutdownOperation = properties.getShutdownOperation();
        return new PrometheusPushGatewayManager(
                this.getPushGateway(properties.getBaseUrl()),
                collectorRegistry,
                pushRate,
                job,
                groupingKey,
                shutdownOperation);
    }

    private PushGateway getPushGateway(String url) throws MalformedURLException {
        return new PushGateway(URI.create(url).toURL());
    }
}
```


## 



# Plus: Alert manager

Queries sobre consumo de memória:

```shell

# consumo de memória HEAP no último minuto
jvm_memory_used_bytes{area="heap"}[1m]

# média do consumo de memória HEAP no  último minuto 
avg_over_time(jvm_memory_used_bytes{area="heap"}[1m]))

# soma da média de consumo de memória
sum(avg_over_time(jvm_memory_used_bytes{area="heap"}[1m]))

# soma da média de consumo de memória, porém agrupando por "application" e "instance"
sum(avg_over_time(jvm_memory_used_bytes{area="heap"}[1m])) by (application,instance)

# percentual de consumo de memória do último minuto
sum(avg_over_time(jvm_memory_used_bytes{area="heap"}[1m]))by(application,instance) * 100 / sum(avg_over_time(jvm_memory_max_bytes{area="heap"}[1m]))by(application,instance) >= 80
```


```shell

# média de uso de conexões sobre o connection pool
sum(avg_over_time(hikaricp_connections_active[1m]))by(application,instance) * 100 / sum(avg_over_time(hikaricp_connections[1m]))by(application, instance)

sum(avg_over_time(jdbc_connections_active[1m]))by(application,instance) * 100 / sum(avg_over_time(jdbc_connections_max[1m]))by(application, instance)
```

```shell
sum(avg_over_time(process_cpu_usage[1m]))by(application,instance)
```


## docs
* https://github.com/prometheus/client_java
* https://github.com/docker/awesome-compose/tree/master/prometheus-grafana
* https://prometheus.io/docs/tutorials/understanding_metric_types/
* https://prometheus.io/docs/prometheus/latest/querying/basics/
