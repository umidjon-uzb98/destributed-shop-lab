
package uz.demo.dockerlab.lockinglab;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;

@Service
public class PessimisticStockWorker {

    private static final Logger log =
            LoggerFactory.getLogger(PessimisticStockWorker.class);

    private final LabProductRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public PessimisticStockWorker(
            LabProductRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public AttemptResult decrease(
            String workerName,
            long productId,
            long holdMillis,
            int lockTimeoutMillis,
            CountDownLatch lockAcquiredSignal
    ) {

        // 1. Ixtiyoriy PostgreSQL lock timeout
        if (lockTimeoutMillis > 0) {

            entityManager.createNativeQuery(
                            "SELECT set_config('lock_timeout', :timeout, true)"
                    )
                    .setParameter(
                            "timeout",
                            lockTimeoutMillis + "ms"
                    )
                    .getSingleResult();
        }

        // 2. Lock olishdan oldingi vaqt
        long startedAt = System.nanoTime();

        // 3. PESSIMISTIC_WRITE lock olamiz
        LabProduct product =
                repository.findByIdForUpdate(productId)
                        .orElseThrow();

        long waitedMillis =
                (System.nanoTime() - startedAt) / 1_000_000;

        int stockBefore = product.getStock();
        Long versionBefore = product.getVersion();

        log.info(
                "{} LOCK ACQUIRED: stock={}, version={}, waitedMs={}",
                workerName,
                stockBefore,
                versionBefore,
                waitedMillis
        );

        // 4. Lock olinganini orchestratorga bildiramiz
        if (lockAcquiredSignal != null) {
            lockAcquiredSignal.countDown();
        }

        // 5. Lockni ataylab ushlab turamiz
        if (holdMillis > 0) {

            try {

                Thread.sleep(holdMillis);

            } catch (InterruptedException ex) {

                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                        "Worker interrupted",
                        ex
                );
            }
        }

        // 6. Biznes operatsiya
        product.decreaseStock();

        // 7. UPDATE SQL ni databasega yuboramiz
        repository.saveAndFlush(product);

        log.info(
                "{} UPDATED: stock {} -> {}, version {} -> {}",
                workerName,
                stockBefore,
                product.getStock(),
                versionBefore,
                product.getVersion()
        );

        return new AttemptResult(
                workerName,
                "SUCCESS",
                stockBefore,
                product.getStock(),
                waitedMillis,
                "Stock decreased"
        );
    }

    public record AttemptResult(
            String worker,
            String status,
            Integer stockBefore,
            Integer stockAfter,
            Long waitedMillis,
            String message
    ) {}
}
