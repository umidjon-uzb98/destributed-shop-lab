package uz.demo.dockerlab.integration.payment;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/simple/payments")
public class SimplePaymentController {
    private final WebClient webClient;
    private final CircuitBreaker breaker =
            CircuitBreaker.of("simplePayment",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(2)
                            .minimumNumberOfCalls(2)
                            .failureRateThreshold(100)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .permittedNumberOfCallsInHalfOpenState(1)
                            .ignoreException(error ->
                                    error instanceof WebClientResponseException.NotFound
                            )
                            .build()
            );
    public SimplePaymentController(
            @Qualifier("paymentWebClient") WebClient webClient
    ) {
        this.webClient = webClient;
    }


    @GetMapping("/{id}")
    public Mono<Payment> getPayment(@PathVariable String id) {

        String requestId = UUID.randomUUID().toString();

        return webClient.get()
                .uri("/api/payments/{id}", id)
                .header("X-Request-ID", requestId)
                .retrieve()
                .bodyToMono(Payment.class)

                // 516: Timeout
                .timeout(Duration.ofSeconds(1))

                // 517: Retry + Backoff + Jitter
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(200))
                                .jitter(0.5)
                                .filter(error ->
                                        error instanceof WebClientResponseException.ServiceUnavailable
                                )
                                .onRetryExhaustedThrow((spec, signal) ->
                                        signal.failure()
                                )
                )

                // Umumiy vaqt chegarasi
                .timeout(Duration.ofSeconds(4))

                // 518: Circuit Breaker
                .transformDeferred(
                        CircuitBreakerOperator.of(breaker)
                )

                // Error Handling
                .onErrorMap(TimeoutException.class, error ->
                        new ResponseStatusException(
                                HttpStatus.GATEWAY_TIMEOUT, "Timeout"
                        )
                )

                .onErrorMap(CallNotPermittedException.class, error ->
                        new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE, "Circuit open"
                        )
                )

                .onErrorMap(WebClientResponseException.class, error ->
                        new ResponseStatusException(
                                error.getStatusCode(), "Payment service error"
                        )
                );
    }
    @GetMapping("/state")
    public String state() {
        return breaker.getState().name();
    }

    public record Payment(String paymentId, String status) {}
}
