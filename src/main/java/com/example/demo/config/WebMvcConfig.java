package com.example.demo.config;

import com.example.demo.security.ActiveAccountInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final ActiveAccountInterceptor activeAccountInterceptor;

    public WebMvcConfig(ActiveAccountInterceptor activeAccountInterceptor) {
        this.activeAccountInterceptor = activeAccountInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activeAccountInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/csrf", "/api/auth/login", "/api/auth/register", "/api/public/**");
    }
}
