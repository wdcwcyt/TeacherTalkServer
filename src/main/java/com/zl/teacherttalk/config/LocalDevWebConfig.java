package com.zl.teacherttalk.config;

import com.zl.teacherttalk.security.LocalDevUserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("local")
public class LocalDevWebConfig implements WebMvcConfigurer {

    private final LocalDevUserInterceptor localDevUserInterceptor;

    public LocalDevWebConfig(LocalDevUserInterceptor localDevUserInterceptor) {
        this.localDevUserInterceptor = localDevUserInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localDevUserInterceptor)
                .addPathPatterns("/api/posts", "/api/posts/**", "/api/auth/me");
    }
}
