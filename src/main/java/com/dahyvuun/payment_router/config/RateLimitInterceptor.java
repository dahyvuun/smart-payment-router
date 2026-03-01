package com.dahyvuun.payment_router.config;

import com.dahyvuun.payment_router.service.RateLimitService;
import com.dahyvuun.payment_router.service.RateLimitService.LimitType;
import com.dahyvuun.payment_router.service.RateLimitService.RateLimitResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String path = request.getRequestURI();
        String identifier = resolveIdentifier(request);
        LimitType limitType = resolveLimitType(path);

        RateLimitResult result = rateLimitService.checkRateLimit(identifier, limitType);

        // 응답 헤더에 Rate Limit 정보 추가 (GitHub API 스타일)
        response.setHeader("X-RateLimit-Limit", String.valueOf(getLimit(limitType)));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.waitSeconds()));

        if (!result.allowed()) {
            // 429 Too Many Requests
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(result.waitSeconds()));

            Map<String, Object> errorBody = Map.of(
                "status", 429,
                "error", "Too Many Requests",
                "message", String.format(
                    "Rate limit exceeded. Try again in %d seconds.", result.waitSeconds()),
                "retryAfterSeconds", result.waitSeconds()
            );

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return false; // 요청 차단
        }

        return true; // 요청 허용
    }

    /**
     * 요청자 식별: 로그인 유저면 이메일, 아니면 IP 사용
     * 유저별로 독립적인 버킷을 가져서 한 유저가 다른 유저에게 영향 없음
     */
    private String resolveIdentifier(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName(); // 이메일
        }
        return getClientIp(request); // IP 주소
    }

    /**
     * URL 경로에 따라 Rate Limit 타입 결정
     */
    private LimitType resolveLimitType(String path) {
        if (path.startsWith("/api/payments")) {
            return LimitType.PAYMENT;
        } else if (path.startsWith("/api/exchange-rates")) {
            return LimitType.EXCHANGE_RATE;
        }
        return LimitType.DEFAULT;
    }

    /**
     * 프록시/로드밸런서 뒤에 있을 때 실제 클라이언트 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private int getLimit(LimitType type) {
        return switch (type) {
            case PAYMENT -> 10;
            case EXCHANGE_RATE -> 30;
            case DEFAULT -> 60;
        };
    }
}