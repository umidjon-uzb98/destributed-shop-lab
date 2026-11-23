
package uz.demo.dockerlab.integration.payment;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentLabMetrics {

    private final MeterRegistry registry;

    public PaymentLabMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void event(String event) {
        registry.counter(
                "lab.payment.events",
                "event",
                event
        ).increment();
    }

    public void recordDuration(long startedNanos) {
        long elapsed =
                System.nanoTime() - startedNanos;

        registry.timer(
                "lab.payment.lookup.duration"
        ).record(
                elapsed,
                TimeUnit.NANOSECONDS
        );
    }
}
