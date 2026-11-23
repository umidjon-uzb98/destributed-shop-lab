
package uz.demo.dockerlab.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "lab.jobs.warmup-enabled",
        havingValue = "true"
)
public class ProductMaintenanceJob {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ProductMaintenanceJob.class
            );

    private final ProductMaintenanceService service;
    private final List<Long> productIds;

    public ProductMaintenanceJob(
            ProductMaintenanceService service,
            @Value("${lab.jobs.warmup-product-ids:}")
            String rawIds
    ) {
        this.service = service;

        this.productIds = Arrays.stream(rawIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .distinct()
                .limit(10)
                .toList();

        if (productIds.stream().anyMatch(id -> id <= 0)) {
            throw new IllegalArgumentException(
                    "Warmup product IDs must be positive"
            );
        }
    }

    @Scheduled(
            initialDelayString =
                    "${lab.jobs.warmup-initial-delay-ms:10000}",
            fixedDelayString =
                    "${lab.jobs.warmup-delay-ms:30000}",
            scheduler = "taskScheduler"
    )
    public void warmup() {

        String previousId = MDC.get("requestId");

        String jobId = "warmup-" + UUID.randomUUID();

        MDC.put("requestId", jobId);

        long start = System.nanoTime();

        try {

            log.info(
                    "WARMUP START: ids={}, thread={}",
                    productIds,
                    Thread.currentThread().getName()
            );

            var result = service.warmCache(productIds);

            log.info(
                    "WARMUP FINISHED: success={}, missing={}, failed={}, durationMs={}",
                    result.succeeded(),
                    result.missing(),
                    result.failed(),
                    (System.nanoTime() - start) / 1_000_000
            );

        } finally {

            if (previousId == null) {
                MDC.remove("requestId");
            } else {
                MDC.put("requestId", previousId);
            }
        }
    }
}
