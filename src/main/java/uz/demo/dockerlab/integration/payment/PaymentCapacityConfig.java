package uz.demo.dockerlab.integration.payment;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class PaymentCapacityConfig {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentCapacityConfig.class);

    @Bean
    public Bulkhead paymentLookupBulkhead() {

        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(2)
                .maxWaitDuration(Duration.ZERO)
                .build();

        Bulkhead bulkhead = Bulkhead.of(
                "paymentLookup",
                config
        );

        bulkhead.getEventPublisher()
                .onCallRejected(event ->
                        log.warn("PAYMENT BULKHEAD REJECTED")
                );

        return bulkhead;
    }

    @Bean
    public RateLimiter paymentLookupRateLimiter() {

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(6)
                .limitRefreshPeriod(Duration.ofSeconds(30))
                .timeoutDuration(Duration.ZERO)
                .build();

        RateLimiter limiter = RateLimiter.of(
                "paymentLookup",
                config
        );

        limiter.getEventPublisher()
                .onFailure(event ->
                        log.warn("PAYMENT RATE LIMIT REJECTED")
                );

        return limiter;
    }

    @Bean
    public TimeLimiter paymentLookupTimeLimiter(
            @Value("${payment.lookup.total-timeout-ms:8000}")
            long timeoutMs
    ) {

        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(timeoutMs))
                .build();

        TimeLimiter limiter = TimeLimiter.of(
                "paymentLookup",
                config
        );

        limiter.getEventPublisher()
                .onTimeout(event ->
                        log.warn("PAYMENT TOTAL DEADLINE EXCEEDED")
                );

        return limiter;
    }
}