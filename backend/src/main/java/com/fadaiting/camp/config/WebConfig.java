package com.fadaiting.camp.config;

import com.fadaiting.camp.interceptors.AdminOperationLogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private AdminOperationLogInterceptor adminOperationLogInterceptor;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c -> true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminOperationLogInterceptor)
                .addPathPatterns("/api/**", "/admin/**", "/booking/**", "/camp/**", "/payment/**", "/notification/**", "/operation-log/**", "/upload/**", "/role/**", "/category/**", "/article/**");
    }
}
