package DeepQuard.DeepGuardBackend.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter authRateLimiter() {
        return RateLimiter.of("auth", RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(5) // 5 attempts per minute
                .timeoutDuration(Duration.ofSeconds(1))
                .build());
    }

    @Bean
    public RateLimiter apiRateLimiter() {
        return RateLimiter.of("api", RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(100) // 100 requests per minute
                .timeoutDuration(Duration.ofSeconds(1))
                .build());
    }
}