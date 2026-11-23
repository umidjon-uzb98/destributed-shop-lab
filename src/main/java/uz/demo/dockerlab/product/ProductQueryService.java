
package uz.demo.dockerlab.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ProductQueryService {

    private static final Logger log =
            LoggerFactory.getLogger(ProductQueryService.class);

    private static final Duration NEGATIVE_TTL =
            Duration.ofSeconds(20);

    private final ProductRepository repository;
    private final Cache productCache;
    private final StringRedisTemplate redisTemplate;
    private final ProductReadMetrics metrics;

    private final long readDelayMs;

    private final ReentrantLock[] locks =
            new ReentrantLock[64];

    private final Semaphore databaseSlots =
            new Semaphore(5);

    public ProductQueryService(
            ProductRepository repository,
            CacheManager cacheManager,
            StringRedisTemplate redisTemplate,
            ProductReadMetrics metrics,
            @Value("${lab.product.read-delay-ms:0}")
            long readDelayMs
    ) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.metrics = metrics;
        this.readDelayMs = readDelayMs;

        this.productCache = Objects.requireNonNull(
                cacheManager.getCache("products"),
                "products cache must exist"
        );

        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public ProductView findById(Long id) {

        var timer = metrics.startTimer();

        try {
            if (id == null || id <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid product ID"
                );
            }

            try {
                return findWithCache(id);

            } catch (CacheUnavailableException e) {

                metrics.fallback();

                log.warn(
                        "DB FALLBACK: productId={}",
                        id
                );

                return readDatabase(id)
                        .orElseThrow(this::notFound);
            }

        } finally {
            metrics.stopTimer(timer);
        }
    }

    private ProductView findWithCache(Long id) {

        // 1. Dastlabki cache tekshiruvi
        ProductView cached = lookupCache(id, true);

        if (cached != null) {
            return cached;
        }

        // 2. Single-flight
        ReentrantLock lock = locks[
                Math.floorMod(id.hashCode(), locks.length)
                ];

        acquireLock(lock);

        try {
            // 3. Double-check: metric ikki marta sanalmaydi
            cached = lookupCache(id, false);

            if (cached != null) {
                return cached;
            }

            // 4. Database'dan o'qish
            Optional<ProductView> product =
                    readDatabase(id);

            if (product.isEmpty()) {

                writeNegativeCache(id);

                throw notFound();
            }

            // 5. Cache'ga yozish
            ProductView result = product.get();

            writePositiveCache(id, result);

            return result;

        } finally {
            lock.unlock();
        }
    }

    private ProductView lookupCache(
            Long id,
            boolean initialLookup
    ) {

        try {
            ProductView cached =
                    productCache.get(id, ProductView.class);

            if (cached != null) {

                if (initialLookup) {
                    metrics.hit();
                }

                log.debug("CACHE HIT: productId={}", id);

                return cached;
            }

            if (Boolean.TRUE.equals(
                    redisTemplate.hasKey(missingKey(id))
            )) {

                if (initialLookup) {
                    metrics.negative();
                }

                log.debug(
                        "NEGATIVE CACHE HIT: productId={}",
                        id
                );

                throw notFound();
            }

            if (initialLookup) {
                metrics.miss();
            }

            return null;

        } catch (RedisConnectionFailureException
                 | QueryTimeoutException e) {

            if (initialLookup) {
                metrics.lookupError();
            }

            recordRedisError("lookup", id, e);

            throw new CacheUnavailableException(e);
        }
    }

    private void acquireLock(ReentrantLock lock) {

        try {
            boolean acquired = lock.tryLock(
                    2, TimeUnit.SECONDS
            );

            if (!acquired) {

                metrics.lockRejected();

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Cache loading is busy"
                );
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Request interrupted",
                    e
            );
        }
    }

    private Optional<ProductView> readDatabase(Long id) {

        if (!databaseSlots.tryAcquire()) {

            metrics.databaseRejected();

            log.warn(
                    "DB READ REJECTED: productId={}",
                    id
            );

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Database read capacity exceeded"
            );
        }

        try {
            metrics.databaseRead();

            log.info(
                    "DATABASE READ: productId={}", id
            );

            simulateSlowRead();

            return repository.findById(id)
                    .map(product -> new ProductView(
                            product.getId(),
                            product.getName()
                    ));

        } finally {
            databaseSlots.release();
        }
    }

    private void writePositiveCache(
            Long id,
            ProductView product
    ) {
        try {
            productCache.put(id, product);

        } catch (RedisConnectionFailureException
                 | QueryTimeoutException e) {

            recordRedisError("positive-write", id, e);
        }
    }

    private void writeNegativeCache(Long id) {

        try {
            redisTemplate.opsForValue().set(
                    missingKey(id),
                    "1",
                    NEGATIVE_TTL
            );

        } catch (RedisConnectionFailureException
                 | QueryTimeoutException e) {

            recordRedisError("negative-write", id, e);
        }
    }

    private void recordRedisError(
            String operation,
            Long id,
            RuntimeException exception
    ) {
        metrics.redisError();

        log.warn(
                "REDIS ERROR: operation={}, productId={}, type={}",
                operation,
                id,
                exception.getClass().getSimpleName()
        );
    }

    private String missingKey(Long id) {
        return "products-missing::" + id;
    }

    private ResponseStatusException notFound() {

        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Product not found"
        );
    }

    // Faqat load test uchun. Default qiymat 0.
    private void simulateSlowRead() {

        if (readDelayMs <= 0) {
            return;
        }

        try {
            Thread.sleep(readDelayMs);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Request interrupted",
                    e
            );
        }
    }

    @Cacheable(
            cacheNames = "product-lists",
            key = "'all'"
    )
    public ProductListView findAll() {

        log.info("DATABASE READ: all products");

        var products = repository.findAll()
                .stream()
                .map(product -> new ProductView(
                        product.getId(),
                        product.getName()
                ))
                .toList();

        return new ProductListView(products);
    }

    private static class CacheUnavailableException
            extends RuntimeException {

        CacheUnavailableException(Throwable cause) {
            super(cause);
        }
    }
}
