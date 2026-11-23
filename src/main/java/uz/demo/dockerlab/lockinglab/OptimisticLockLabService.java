
package uz.demo.dockerlab.lockinglab;

import jakarta.persistence.OptimisticLockException;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.*;

@Service
public class OptimisticLockLabService {

    private static final long PRODUCT_ID = 1L;

    private final LabProductRepository repository;
    private final StockTransactionWorker worker;

    public OptimisticLockLabService(
            LabProductRepository repository,
            StockTransactionWorker worker
    ) {
        this.repository = repository;
        this.worker = worker;
    }

    // Test mahsulotini tayyorlash
    @Transactional
    public ProductState reset(int stock) {

        LabProduct product = repository.findById(PRODUCT_ID)
                .orElseGet(() -> new LabProduct(
                        PRODUCT_ID,
                        "Mechanical Keyboard",
                        stock
                ));

        product.setStock(stock);

        LabProduct saved = repository.saveAndFlush(product);

        return toState(saved);
    }

    // Hozirgi holatni ko'rish
    public ProductState currentState() {

        LabProduct product = repository.findById(PRODUCT_ID)
                .orElseThrow();

        return toState(product);
    }

    // Ikkita parallel transaction
    public RaceResult runRace() {
        ProductState before = currentState();

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor =
                Executors.newFixedThreadPool(2);
        try {
            Future<StockTransactionWorker.AttemptInfo> first =
                    executor.submit(
                            () -> safeDecrease(barrier)
                    );

            Future<StockTransactionWorker.AttemptInfo> second =
                    executor.submit(
                            () -> safeDecrease(barrier)
                    );

            var resultA = first.get();
            var resultB = second.get();

            ProductState after = currentState();

            return new RaceResult(
                    before,
                    List.of(resultA, resultB),
                    after
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Race test interrupted",
                    ex
            );

        } catch (ExecutionException ex) {

            throw new IllegalStateException(
                    "Race test failed",
                    ex
            );

        } finally {

            executor.shutdownNow();
        }
    }

    private StockTransactionWorker.AttemptInfo safeDecrease(
            CyclicBarrier barrier
    ) {

        try {

            return worker.decrease(
                    PRODUCT_ID,
                    barrier
            );

        } catch (
                OptimisticLockingFailureException |
                OptimisticLockException ex
        ) {

            return new StockTransactionWorker.AttemptInfo(
                    "CONFLICT",
                    null,
                    null,
                    null,
                    "Version changed by another transaction"
            );

        } catch (Exception ex) {

            return new StockTransactionWorker.AttemptInfo(
                    "ERROR",
                    null,
                    null,
                    null,
                    ex.getClass().getSimpleName()
                            + ": " + ex.getMessage()
            );
        }
    }

    private ProductState toState(LabProduct product) {

        return new ProductState(
                product.getId(),
                product.getName(),
                product.getStock(),
                product.getVersion()
        );
    }

    public record ProductState(
            Long id,
            String name,
            int stock,
            Long version
    ) {}

    public record RaceResult(
            ProductState before,
            List<StockTransactionWorker.AttemptInfo> attempts,
            ProductState after
    ) {}
}
