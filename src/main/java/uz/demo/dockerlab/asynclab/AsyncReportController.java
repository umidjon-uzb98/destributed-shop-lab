
package uz.demo.dockerlab.asynclab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/async-lab/reports")
public class AsyncReportController {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AsyncReportController.class
            );

    private final AsyncReportService service;

    public AsyncReportController(
            AsyncReportService service
    ) {
        this.service = service;
    }

    @GetMapping("/summary")
    public CompletableFuture<ReportResult> summary(
            @RequestParam(defaultValue = "false")
            boolean fail
    ) {

        long start = System.nanoTime();

        try {
            CompletableFuture<Integer> orders =
                    service.countOrders();

            CompletableFuture<Long> sales =
                    service.calculateSales(fail);

            return orders.thenCombine(
                    sales,
                    ReportResult::new

            ).whenComplete((result, error) -> {

                long durationMs =
                        (System.nanoTime() - start)
                                / 1_000_000;

                if (error != null) {
                    log.error(
                            "REPORT FAILED: durationMs={}",
                            durationMs,
                            error
                    );
                } else {
                    log.info(
                            "REPORT COMPLETED: durationMs={}",
                            durationMs
                    );
                }
            });

        } catch (TaskRejectedException e) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Report executor is busy"
            );
        }
    }

    public record ReportResult(
            Integer orderCount,
            Long totalSales
    ) {}
}
