## passo 1
**Implementar a interface de logs em cada classe.**

Utilize um objeto estático para garantir reuso para todas as instâncias

```java
private static final Logger log = LoggerFactory.getLogger(NomeDaClasse.class);
```

## passo 2
**Implementar os logs conforme necessário**

```java
// o método permite interpolar certo conteúdo com o log
log.info("mensagem do log - parametro: {}", objeto);
log.info("mensagem do log - p1: {}, p2: {}", arg1, arg2);

// logs de debug
log.debug("agora somando os dois números: {} + {} = {}", v1, v2, v3);

// exemplo de exceção
log.error("mensagem de erro - detalhe da exceção: {}", ex);
```


## passo 3
**Usar uma biblioteca específica**

No hands on vamos usar o LogBack, e as suas dependências são as seguintes:

```xml
<!-- LogBack core, possui as funcionalidades básicas de escrita e formatação -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-core</artifactId>
    <version>1.5.6</version>
</dependency>

<!-- funções mais sofisticadas de logging, como formatação em JSON -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

## passo 4
**Usar `MDC` para adicionar dados de contexto.**

Para aplicações Web/API, você pode usar um interceptor nativo do framework - ex: `HandlerInterceptor` do Spring (Java); já aplicações "worker", como batch ou que recebem estímulos de fila ou tópico, você pode usar fazer o enriquecimento do MDC no primeiro ponto de contato da aplicação - ex: classe/método `main()`

```java
@Component
class RequestMdcInterceptor implements HandlerInterceptor {

    // capturando propriedades geradas no build
    @Autowired
    private BuildProperties buildProperties;

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler) throws Exception {
        MDC.put("appName", buildProperties.getName());
        MDC.put("appVersion", buildProperties.getVersion());
        MDC.put("appBuildDate", buildProperties.getTime().toString());
        MDC.put("traceId", request.getHeader("x-trace-id"));
        MDC.put("host", request.getHeader("Host"));
        return true;
    }
}

// no caso do Spring, é necessário registrar o intereptor para ser reconhecido
@Configuration
class InterceptorsAdapterConfig implements WebMvcConfigurer {

    @Autowired
    private RequestMdcInterceptor requestMdcInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(requestMdcInterceptor);
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
```

Para capturar os dados de build através da classe `BuildProperties`, é necessário incluir mais alguns dados de build no plugin do maven.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <!-- execution para gerar o arquivo de metadados
                     que a classe BuildProperties vai ler -->
                <execution>
                    <id>build-info</id>
                    <goals>
                        <goal>build-info</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```



## passo 5
**Aplicar as configurações de formato e destino dos logs.**

No caso do LogBack, uma das formas de instrumentação é através da parametrização do arquivo `src/main/resources/logback.xml`, ex:

```xml
<configuration>
    <!-- é possível importar as classes em vez de referenciar toda vez o caminho completo -->
    <import class="ch.qos.logback.core.ConsoleAppender" />
    <import class="ch.qos.logback.core.FileAppender" />
    <import class="ch.qos.logback.classic.encoder.PatternLayoutEncoder" />
    <import class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder" />

     <timestamp key="timestamp" datePattern="yyyyMMdd'T'HHmm"/>

    <!-- appender específico para arquivo -->
    <appender name="FILE" class="FileAppender">
        <!-- local onde o arquivo será gravado
            OBS: é possível adicionar varávis de ambiente para customizar o nome e o local do arquivo gerado (ex: data/hora)
        -->
        <file>logs/application-${timestamp}.log</file>
        <encoder class="PatternLayoutEncoder">
            <!-- 
            pattern de escrita de cada linha de log
                * %d{HH:mm:ss.SSSZZ} - timestamp do evento de log
                * [%thread] - thread da aplicação em que o log foi gerado
                * %-5level - nível de log (ERROR, INFO, DEBUG, etc.)
                * %X{traceId} - variável específica "traceID" no MDC
                                poderia colocar %X para exibir todas
                                as variáveis
                * %X{host} - variável específica "host" no MDC
                            poderia colocar %X para exibir todas
                            as variáveis
                * %logger{36} - classe em que o log foi gerado
                                nome limitado a 36 caracteres
                * %msg%n - mensagem acompanhada de exceção + stack trace
            -->
            <pattern>
                %d{HH:mm:ss.SSSZZ} [%thread] %-5level %X{traceId} %X{host} %logger{36} - %msg%n
            </pattern>
        </encoder>
    </appender>

    <!-- é possível ter múltiplos appenders -->
    <appender name="STDOUT_PLAIN" class="ConsoleAppender">
        <encoder class="PatternLayoutEncoder">
            <!-- ... e com formatos diferentes -->
            <pattern>
                %d{HH:mm:ss.SSSZZ} [%thread] %-5level %X %logger{36} - %msg%n
            </pattern>
        </encoder>
    </appender>

    <appender name="STDOUT_JSON" class="ConsoleAppender">
        <!-- classe net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder permite usar formato JSON -->
        <encoder class="LoggingEventCompositeJsonEncoder">
            <providers>
                <!-- a biblioteca conhece o campo timestamp... -->
                <timestamp>
                    <!-- ... mas é possível customizar o nome e até o formato -->
                    <fieldName>ts</fieldName>
                    <timeZone>UTC</timeZone>
                </timestamp>
                <loggerName>
                    <fieldName>logger</fieldName>
                </loggerName>
                <logLevel>
                    <fieldName>level</fieldName>
                </logLevel>
                <callerData>
                    <classFieldName>class</classFieldName>
                    <methodFieldName>method</methodFieldName>
                    <lineFieldName>line</lineFieldName>
                    <fileFieldName>file</fileFieldName>
                </callerData>
                <threadName>
                    <fieldName>thread</fieldName>
                </threadName>
                <mdc />
                <stackTrace>
                    <fieldName>stack</fieldName>
                </stackTrace>
                <message>
                    <fieldName>msg</fieldName>
                </message>

                <!-- 
                    é possível capturar argumentos dos métodos em que o log foi registrado
                    OBS: cuidado com as políticas de segurança e privacidade de dados  -->
                <arguments>
                    <includeNonStructuredArguments>true</includeNonStructuredArguments>
                    <nonStructuredArgumentsFieldPrefix>argument:</nonStructuredArgumentsFieldPrefix>
                </arguments>
            </providers>
        </encoder>
    </appender>

    <!-- 
        mesmo tendo uma configuração global de nível de log, 
        é possível customizar os níveis segundo o package de cada
        classe que estive logando -->
    <logger name="ch.qos.logback" level="WARN" />
    <logger name="org.mortbay.log" level="WARN" />
    <logger name="org.springframework" level="INFO" />
    <logger name="org.springframework.beans" level="WARN" />

    <!-- por exemplo para a aplicação estou habilitando a opção de DEBUG -->
    <logger name="store" level="DEBUG" />

    <!-- 
        aqui é onde você habilita/desabilita quais Appenders vai utilizar
        o que facilita a manutenção, sem ter que ficar movendo código -->
    <root level="INFO">
        <!-- OBS: Appender STDOUT_PLAIN está desativado-->
        <!--<appender-ref ref="STDOUT_PLAIN" />-->

        <!-- é possível utilizar vários Appenders simultaneamente -->
        <appender-ref ref="STDOUT_JSON" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```


# Plus: integrando ao Elastic Search

## passo 1
Instale o pluging do Elastic Search para integrá-lo ao log driver.

```sh
docker plugin install elastic/elastic-logging-plugin:8.7.1
```

## passo 3 - subir a infra
Suba a infra do ELK conforme a documentação oficial:
https://www.elastic.co/blog/getting-started-with-the-elastic-stack-and-docker-compose

Esse procedimento irá provisionar a infraestrutura com Logstash, Elastic Search e Kibana.

## passo 3
Configure o LogDriver para a aplicação no `docker-compose.yml`, utilizando os parâmetros obtidos na instalação do ELK.

```yaml
services:
  app:
    logging:
    driver: elastic/elastic-logging-plugin:8.7.1
    options:
      hosts: https://localhost:9200
      user: elastic
      password: changeme
      index: storeapp
```
