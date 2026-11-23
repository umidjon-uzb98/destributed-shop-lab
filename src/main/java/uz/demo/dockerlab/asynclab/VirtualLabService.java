
package uz.demo.dockerlab.asynclab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

@Service
public class VirtualLabService {

    private static final Logger log =
            LoggerFactory.getLogger(VirtualLabService.class);

    private final Semaphore ioSlots =
            new Semaphore(10);

    @Async("virtualExecutor")
    public CompletableFuture<VirtualResult> execute() {

        if (!ioSlots.tryAcquire()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "External I/O capacity exceeded"
            );
        }

        try {

            Thread thread = Thread.currentThread();

            String requestId = MDC.get("requestId");

            log.info(
                    "VIRTUAL START: thread={}, virtual={}, requestId={}",
                    thread.getName(),
                    thread.isVirtual(),
                    requestId
            );

            simulateExternalIo();

            log.info(
                    "VIRTUAL FINISHED: requestId={}",
                    requestId
            );

            return CompletableFuture.completedFuture(
                    new VirtualResult(
                            thread.getName(),
                            thread.isVirtual(),
                            requestId,
                            "COMPLETED"
                    )
            );

        } finally {
            ioSlots.release();
        }
    }

    private void simulateExternalIo() {

        try {
            Thread.sleep(1000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Virtual task interrupted", e
            );
        }
    }

    public record VirtualResult(
            String threadName,
            boolean virtual,
            String requestId,
            String status
    ) {}
}
