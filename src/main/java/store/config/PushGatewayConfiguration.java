package store.config;

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
        String jobName = appName;
        Map<String, String> groupingKey = properties.getGroupingKey();
        PrometheusPushGatewayManager.ShutdownOperation shutdownOperation = properties.getShutdownOperation();
        return new PrometheusPushGatewayManager(
                this.getPushGateway(properties.getBaseUrl()),
                collectorRegistry,
                pushRate,
                jobName,
                groupingKey,
                shutdownOperation);
    }

    private PushGateway getPushGateway(String url) throws MalformedURLException {
        return new PushGateway(URI.create(url).toURL());
    }
}