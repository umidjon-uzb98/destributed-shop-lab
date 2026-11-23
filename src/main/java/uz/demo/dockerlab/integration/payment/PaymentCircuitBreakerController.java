
package uz.demo.dockerlab.integration.payment;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentCircuitBreakerController {

    private final CircuitBreaker breaker;

    public PaymentCircuitBreakerController(
            @Qualifier("paymentLookupBreaker")
            CircuitBreaker breaker
    ) {
        this.breaker = breaker;
    }

    @GetMapping("/api/integration-lab/circuit-breaker/payment")
    public BreakerStats stats() {

        CircuitBreaker.Metrics metrics =
                breaker.getMetrics();

        return new BreakerStats(

                breaker.getName(),

                breaker.getState().name(),

                metrics.getNumberOfBufferedCalls(),

                metrics.getNumberOfSuccessfulCalls(),

                metrics.getNumberOfFailedCalls(),

                metrics.getFailureRate(),

                metrics.getNumberOfNotPermittedCalls()
        );
    }

    public record BreakerStats(
            String name,
            String state,
            int bufferedCalls,
            int successfulCalls,
            int failedCalls,
            float failureRate,
            long notPermittedCalls
    ) {}
}
