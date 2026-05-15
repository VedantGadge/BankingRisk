package com.example.bankingproject.common.ratelimit;

import com.example.bankingproject.common.ratelimit.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Intercepts incoming API requests and enforces a rate limit using Redis.
 * 
 * Uses a Fixed Window algorithm:
 * Key = rate_limit:{userId} or rate_limit:{ip}
 * Increments the key. If it reaches 1, sets a 1-minute expiration.
 * If it exceeds the limit, blocks the request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    
    // Limits: 20 requests per minute per user
    private static final int MAX_REQUESTS_PER_MINUTE = 20;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        
        String identifier = resolveClientIdentifier(request);
        String redisKey = "rate_limit:" + identifier;

        try {
            Long requestCount = redisTemplate.opsForValue().increment(redisKey);

            if (requestCount != null && requestCount == 1) {
                // First request in the window, set the 1-minute expiration
                redisTemplate.expire(redisKey, Duration.ofMinutes(1));
            }

            if (requestCount != null && requestCount > MAX_REQUESTS_PER_MINUTE) {
                log.warn("[rate-limit] Blocked request from {}. Exceeded {} requests/min", identifier, MAX_REQUESTS_PER_MINUTE);
                throw new RateLimitExceededException("Too many requests. Please try again later.");
            }
            
            // Add remaining rate limit info to headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - (requestCount != null ? requestCount : 0))));
            
        } catch (RedisConnectionFailureException e) {
            // If Redis is down, we "fail open" (allow the request) so the app doesn't crash completely.
            // In a strict banking app, you might choose to "fail closed" (block the request).
            log.warn("[rate-limit] Redis is down, bypassing rate limit for {}", identifier);
        }

        return true;
    }

    /**
     * Resolves the user's ID if authenticated, otherwise falls back to their IP address.
     */
    private String resolveClientIdentifier(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "user_" + auth.getPrincipal().toString();
        }
        
        // Fallback to IP address for unauthenticated endpoints (like login/register)
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return "ip_" + xfHeader.split(",")[0];
        }
        return "ip_" + request.getRemoteAddr();
    }
}
