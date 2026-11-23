package uz.demo.dockerlab.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import uz.demo.dockerlab.product.ProductChangedEvent;

import java.util.List;

@Component
public class ProductCacheInvalidationListener {
    private static final Logger log = LoggerFactory.getLogger(ProductCacheInvalidationListener.class);
    private final StringRedisTemplate redisTemplate;

    public ProductCacheInvalidationListener(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductChanged(
            ProductChangedEvent event
    ) {

        String productKey =
                "products::" + event.productId();

        String missingKey =
                "products-missing::" + event.productId();

        String listKey = "product-lists::all";

        Long deleted = redisTemplate.delete(
                List.of(productKey, missingKey, listKey)
        );

        log.info(
                "CACHE INVALIDATED AFTER COMMIT: " +
                        "productId={}, deletedKeys={}",
                event.productId(),
                deleted
        );
    }
}

