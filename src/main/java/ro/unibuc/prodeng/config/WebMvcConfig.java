package ro.unibuc.prodeng.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ro.unibuc.prodeng.metrics.ErrorInterceptor;
import ro.unibuc.prodeng.metrics.MetricsService;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private MetricsService metricsService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ErrorInterceptor(metricsService));
    }
}
