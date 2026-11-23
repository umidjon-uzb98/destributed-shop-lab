
package uz.demo.dockerlab.lockinglab;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.*;

@Service
public class PessimisticLockLabService {

    private static final long PRODUCT_ID = 1L;

    private final PessimisticStockWorker worker;
    private final OptimisticLockLabService productService;

    public PessimisticLockLabService(
            PessimisticStockWorker worker,
            OptimisticLockLabService productService
    ) {
        this.worker = worker;
        this.productService = productService;
    }

    public RaceResult runRace(boolean useLockTimeout) {

        var before = productService.currentState();

        CountDownLatch firstLockAcquired =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {

            // Transaction A: lockni 4 sekund ushlab turadi
            Future<PessimisticStockWorker.AttemptResult> first =
                    executor.submit(
                            () -> safeDecrease(
                                    "A",
                                    4000,
                                    0,
                                    firstLockAcquired
                            )
                    );

            // A lockni olishini kutamiz
            boolean acquired = firstLockAcquired.await(
                    10,
                    TimeUnit.SECONDS
            );

            if (!acquired) {
                throw new IllegalStateException(
                        "First worker did not acquire the lock"
                );
            }

            // Transaction B: A lockni olgandan keyin boshlanadi
            int timeoutMillis =
                    useLockTimeout ? 1000 : 0;

            Future<PessimisticStockWorker.AttemptResult> second =
                    executor.submit(
                            () -> safeDecrease(
                                    "B",
                                    0,
                                    timeoutMillis,
                                    null
                            )
                    );

            var resultA = first.get(
                    15,
                    TimeUnit.SECONDS
            );

            var resultB = second.get(
                    15,
                    TimeUnit.SECONDS
            );

            var after = productService.currentState();

            return new RaceResult(
                    before,
                    List.of(resultA, resultB),
                    after
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Concurrency test interrupted",
                    ex
            );

        } catch (ExecutionException | TimeoutException ex) {

            throw new IllegalStateException(
                    "Concurrency test failed",
                    ex
            );

        } finally {

            executor.shutdownNow();
        }
    }

    private PessimisticStockWorker.AttemptResult safeDecrease(
            String name,
            long holdMillis,
            int lockTimeoutMillis,
            CountDownLatch lockAcquiredSignal
    ) {

        long startedAt = System.nanoTime();

        try {

            return worker.decrease(
                    name,
                    PRODUCT_ID,
                    holdMillis,
                    lockTimeoutMillis,
                    lockAcquiredSignal
            );

        } catch (Exception ex) {

            long elapsedMillis =
                    (System.nanoTime() - startedAt) / 1_000_000;

            String status;

            if (hasSqlState(ex, "55P03")) {

                status = "LOCK_TIMEOUT";

            } else if (hasSqlState(ex, "40P01")) {

                status = "DEADLOCK";

            } else {

                status = "ERROR";
            }

            return new PessimisticStockWorker.AttemptResult(
                    name,
                    status,
                    null,
                    null,
                    elapsedMillis,
                    ex.getClass().getSimpleName()
            );
        }
    }

    private boolean hasSqlState(
            Throwable error,
            String sqlState
    ) {

        Throwable current = error;

        while (current != null) {

            if (current instanceof SQLException sqlEx
                    && sqlState.equals(sqlEx.getSQLState())) {

                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    public record RaceResult(
            OptimisticLockLabService.ProductState before,
            List<PessimisticStockWorker.AttemptResult> attempts,
            OptimisticLockLabService.ProductState after
    ) {}
}
