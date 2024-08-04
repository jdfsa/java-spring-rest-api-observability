package store.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.text.MessageFormat;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
class RequestMetricsInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler) throws Exception {

        final HandlerMethod handlerMethod = (HandlerMethod) handler;
        final RequestMapping requestMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
        if (Objects.isNull(requestMapping))
            return true;

        final String path = requestMapping.value()[0];
        final String method = requestMapping.method()[0].name();

        final Counter counter = Counter.builder(
                String.format(
                        "api_request_%s%s",
                        method.toLowerCase(),
                        path.replaceAll("[/{}]", "_")
                                .replace("__", "_")
                ))
                .description(String.format("number of requests for %s %s", method, path))
                .register(meterRegistry);
        counter.increment();
        return true;
    }
}
