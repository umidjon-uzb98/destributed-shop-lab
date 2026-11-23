
package uz.demo.dockerlab.lockinglab;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class StockTransactionWorker {

    private final LabProductRepository repository;

    public StockTransactionWorker(
            LabProductRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public AttemptInfo decrease(
            long productId,
            CyclicBarrier barrier
    ) {

        // 1. Entity'ni o'qiymiz
        LabProduct product = repository.findById(productId)
                .orElseThrow();

        int stockBefore = product.getStock();
        Long versionBefore = product.getVersion();

        // 2. Faqat concurrency laboratoriyasi uchun
        if (barrier != null) {
            waitForOtherTransaction(barrier);
        }

        // 3. Stock kamaytiramiz
        product.decreaseStock();

        // 4. Hibernate UPDATE'ni hozir bajarishga urinadi
        repository.saveAndFlush(product);

        return new AttemptInfo(
                "SUCCESS",
                stockBefore,
                versionBefore,
                product.getStock(),
                "Update succeeded"
        );
    }

    private void waitForOtherTransaction(
            CyclicBarrier barrier
    ) {

        try {

            barrier.await(10, TimeUnit.SECONDS);

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Concurrency test interrupted",
                    ex
            );

        } catch (
                BrokenBarrierException | TimeoutException ex
        ) {

            throw new IllegalStateException(
                    "Concurrency test synchronization failed",
                    ex
            );
        }
    }

    public record AttemptInfo(
            String status,
            Integer stockRead,
            Long versionRead,
            Integer stockAfter,
            String message
    ) {}
}
