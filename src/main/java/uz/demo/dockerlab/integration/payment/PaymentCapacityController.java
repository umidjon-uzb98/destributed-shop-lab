package uz.demo.dockerlab.integration.payment;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.ratelimiter.RateLimiter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentCapacityController {

    private final Bulkhead bulkhead;
    private final RateLimiter rateLimiter;

    public PaymentCapacityController(
            @Qualifier("paymentLookupBulkhead")
            Bulkhead bulkhead,

            @Qualifier("paymentLookupRateLimiter")
            RateLimiter rateLimiter
    ) {
        this.bulkhead = bulkhead;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/api/integration-lab/capacity/payment")
    public CapacityStats stats() {

        int available =
                bulkhead.getMetrics()
                        .getAvailableConcurrentCalls();

        int max = bulkhead.getBulkheadConfig()
                .getMaxConcurrentCalls();

        return new CapacityStats(
                max,
                available,
                max - available,
                rateLimiter.getMetrics()
                        .getAvailablePermissions()
        );
    }

    public record CapacityStats(
            int maxConcurrentCalls,
            int availableConcurrentCalls,
            int inUse,
            int availableRatePermissions
    ) {}
}