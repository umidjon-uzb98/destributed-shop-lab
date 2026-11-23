
package uz.demo.dockerlab.asynclab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncReportService {

    private static final Logger log =
            LoggerFactory.getLogger(AsyncReportService.class);

    @Async("reportExecutor")
    public CompletableFuture<Integer> countOrders() {

        log.info(
                "COUNT ORDERS START: thread={}",
                Thread.currentThread().getName()
        );

        simulateWork();

        return CompletableFuture.completedFuture(120);
    }

    @Async("reportExecutor")
    public CompletableFuture<Long> calculateSales(
            boolean fail
    ) {

        log.info(
                "CALCULATE SALES START: thread={}",
                Thread.currentThread().getName()
        );

        simulateWork();

        if (fail) {
            throw new IllegalStateException(
                    "Simulated sales report failure"
            );
        }

        return CompletableFuture.completedFuture(
                7_500_000L
        );
    }

    private void simulateWork() {

        try {
            Thread.sleep(1000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Report interrupted", e
            );
        }
    }
}
