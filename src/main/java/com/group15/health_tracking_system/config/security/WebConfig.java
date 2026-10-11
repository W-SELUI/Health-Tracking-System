package com.group15.health_tracking_system.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ProviderVerificationInterceptor
            providerVerificationInterceptor;

    public WebConfig(
            ProviderVerificationInterceptor providerVerificationInterceptor) {

        this.providerVerificationInterceptor =
                providerVerificationInterceptor;
    }

    @Override
    public void addInterceptors(
            InterceptorRegistry registry) {

        registry.addInterceptor(
                        providerVerificationInterceptor)
                .addPathPatterns(
                        "/provider/**")
                .excludePathPatterns(
                        "/provider/verification");
    }
}