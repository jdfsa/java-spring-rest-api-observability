package store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
class InterceptorsAdapterConfig implements WebMvcConfigurer {

    private final RequestMdcInterceptor requestMdcInterceptor;
    private final RequestMetricsInterceptor requestMetricsInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(requestMdcInterceptor);
        registry.addInterceptor(requestMetricsInterceptor);
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
