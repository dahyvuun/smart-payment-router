package com.dahyvuun.payment_router.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            // Rate Limit 적용 경로
            .addPathPatterns("/api/**")
            // 제외 경로: 인증, actuator, 어드민 모니터링
            .excludePathPatterns(
                "/api/auth/**",              // 회원가입/로그인은 제외 (별도 처리)
                "/actuator/**",              // 헬스체크 제외
                "/api/v1/admin/**"           // 어드민 모니터링 제외
            );
    }
}