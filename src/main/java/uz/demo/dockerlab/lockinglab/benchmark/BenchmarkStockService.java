
package uz.demo.dockerlab.lockinglab.benchmark;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BenchmarkStockService {

    private static final long PRODUCT_ID = 1L;
    private static final long DEMO_DELAY_MS = 70L;

    private final JdbcTemplate jdbcTemplate;
    private final BenchmarkProductRepository repository;

    public BenchmarkStockService(
            JdbcTemplate jdbcTemplate,
            BenchmarkProductRepository repository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
    }

    // Har bir scenario boshida stock=10, version=0
    public StockState reset() {

        jdbcTemplate.update("""
                INSERT INTO locking_benchmark_product
                    (id, stock, version)
                VALUES (1, 10, 0)
                ON CONFLICT (id)
                DO UPDATE SET
                    stock = 10,
                    version = 0
                """);

        return currentStock();
    }

    public StockState currentStock() {

        return jdbcTemplate.queryForObject("""
                SELECT stock, version
                FROM locking_benchmark_product
                WHERE id = ?
                """,
                (rs, rowNum) -> new StockState(
                        rs.getInt("stock"),
                        rs.getLong("version")
                ),
                PRODUCT_ID
        );
    }

    // =========================================
    // STRATEGY 1: WITHOUT LOCKING (UNSAFE)
    // =========================================
    @Transactional
    public BuyResult buyUnsafe() {

        Integer stock = jdbcTemplate.queryForObject(
                """
                SELECT stock
                FROM locking_benchmark_product
                WHERE id = ?
                """,
                Integer.class,
                PRODUCT_ID
        );

        if (stock == null || stock <= 0) {
            return new BuyResult(
                    "unsafe",
                    "SOLD_OUT",
                    stock
            );
        }

        // Race condition oynasini kengaytiramiz
        demoDelay();

        // ATAYLAB XAVFLI:
        // Eski stock asosida absolute update.
        // Version tekshiruvi yo'q!
        jdbcTemplate.update(
                """
                UPDATE locking_benchmark_product
                SET stock = ?
                WHERE id = ?
                """,
                stock - 1,
                PRODUCT_ID
        );

        return new BuyResult(
                "unsafe",
                "SUCCESS",
                stock
        );
    }

    // =========================================
    // STRATEGY 2: OPTIMISTIC LOCKING
    // =========================================
    @Transactional
    public BuyResult buyOptimistic() {

        BenchmarkProduct product = repository
                .findById(PRODUCT_ID)
                .orElseThrow();

        if (product.isSoldOut()) {
            return new BuyResult(
                    "optimistic",
                    "SOLD_OUT",
                    product.getStock()
            );
        }

        demoDelay();

        product.decreaseStock();

        // UPDATE + WHERE version = oldVersion
        repository.saveAndFlush(product);

        return new BuyResult(
                "optimistic",
                "SUCCESS",
                product.getStock()
        );
    }

    // =========================================
    // STRATEGY 3: PESSIMISTIC LOCKING
    // =========================================
    @Transactional
    public BuyResult buyPessimistic() {

        BenchmarkProduct product = repository
                .findForUpdate(PRODUCT_ID)
                .orElseThrow();

        if (product.isSoldOut()) {
            return new BuyResult(
                    "pessimistic",
                    "SOLD_OUT",
                    product.getStock()
            );
        }

        // Bu yerda delay lock ushlab turgan
        // transaction ichida bajariladi.
        demoDelay();

        product.decreaseStock();

        repository.saveAndFlush(product);

        return new BuyResult(
                "pessimistic",
                "SUCCESS",
                product.getStock()
        );
    }

    private void demoDelay() {

        try {
            Thread.sleep(DEMO_DELAY_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Benchmark interrupted",
                    ex
            );
        }
    }

    public record StockState(
            int stock,
            long version
    ) {}

    public record BuyResult(
            String strategy,
            String status,
            Integer observedStock
    ) {}
}
