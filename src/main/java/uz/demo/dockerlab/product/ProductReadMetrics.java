package uz.demo.dockerlab.product;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class ProductReadMetrics {

    private final MeterRegistry registry;

    private final Counter hit;
    private final Counter miss;
    private final Counter negative;
    private final Counter lookupError;

    private final Counter databaseReads;
    private final Counter redisErrors;
    private final Counter fallbackRequests;
    private final Counter databaseRejected;
    private final Counter lockRejected;

    private final Timer readDuration;

    public ProductReadMetrics(MeterRegistry registry) {
        this.registry = registry;

        hit = registry.counter(
                "lab.product.cache.lookups",
                "result", "hit"
        );

        miss = registry.counter(
                "lab.product.cache.lookups",
                "result", "miss"
        );

        negative = registry.counter(
                "lab.product.cache.lookups",
                "result", "negative"
        );

        lookupError = registry.counter(
                "lab.product.cache.lookups",
                "result", "error"
        );

        databaseReads = registry.counter(
                "lab.product.db.reads"
        );

        redisErrors = registry.counter(
                "lab.product.redis.errors"
        );

        fallbackRequests = registry.counter(
                "lab.product.fallback.requests"
        );

        databaseRejected = registry.counter(
                "lab.product.db.rejected"
        );

        lockRejected = registry.counter(
                "lab.product.lock.rejected"
        );

        readDuration = registry.timer(
                "lab.product.read.duration"
        );
    }

    public void hit() {
        hit.increment();
    }

    public void miss() {
        miss.increment();
    }

    public void negative() {
        negative.increment();
    }

    public void lookupError() {
        lookupError.increment();
    }

    public void databaseRead() {
        databaseReads.increment();
    }

    public void redisError() {
        redisErrors.increment();
    }

    public void fallback() {
        fallbackRequests.increment();
    }

    public void databaseRejected() {
        databaseRejected.increment();
    }

    public void lockRejected() {
        lockRejected.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(readDuration);
    }
}

