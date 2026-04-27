package ro.unibuc.prodeng.metrics;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

public class ErrorInterceptor implements HandlerInterceptor {

    private final MetricsService metricsService;

    public ErrorInterceptor(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        if (ex != null) {
            metricsService.recordError(ex.getClass().getSimpleName());
        }
    }
}
