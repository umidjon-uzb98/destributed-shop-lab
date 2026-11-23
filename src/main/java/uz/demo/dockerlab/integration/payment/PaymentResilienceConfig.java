
package uz.demo.dockerlab.integration.payment;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class PaymentResilienceConfig {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PaymentResilienceConfig.class
            );

    @Bean("paymentLookupBreaker")
    public CircuitBreaker paymentLookupBreaker() {

        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()

                        .slidingWindowType(
                                CircuitBreakerConfig
                                        .SlidingWindowType.COUNT_BASED
                        )

                        .slidingWindowSize(4)

                        .minimumNumberOfCalls(4)

                        .failureRateThreshold(50)

                        .waitDurationInOpenState(
                                Duration.ofSeconds(30)
                        )

                        .permittedNumberOfCallsInHalfOpenState(2)

                        .maxWaitDurationInHalfOpenState(
                                Duration.ofSeconds(30)
                        )

                        .automaticTransitionFromOpenToHalfOpenEnabled(false)

                        .ignoreException(
                                error -> !isDependencyFailure(error)
                        )

                        .build();

        CircuitBreaker breaker = CircuitBreaker.of(
                "paymentLookup",
                config
        );

        breaker.getEventPublisher()

                .onStateTransition(event ->
                        log.warn(
                                "PAYMENT CIRCUIT TRANSITION: {}",
                                event.getStateTransition()
                        )
                )

                .onCallNotPermitted(event ->
                        log.warn(
                                "PAYMENT CIRCUIT CALL REJECTED"
                        )
                );

        return breaker;
    }

    private boolean isDependencyFailure(Throwable error) {

        if (!(error instanceof PaymentExternalException e)) {
            return false;
        }

        return switch (e.getKind()) {

            case TIMEOUT,
                 UNAVAILABLE,
                 UPSTREAM_FAILURE,
                 INVALID_RESPONSE -> true;

            default -> false;
        };
    }
}
