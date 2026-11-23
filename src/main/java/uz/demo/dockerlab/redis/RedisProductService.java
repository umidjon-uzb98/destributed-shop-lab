package uz.demo.dockerlab.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class RedisProductService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisProductService(
            @Qualifier("readableRedisTemplate")
            RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Long id, ProductData product) {
        String key = "lab:product:" + id;

        redisTemplate.opsForHash().putAll(
                key,
                Map.of(
                        "name", product.name(),
                        "price", product.price().toString()
                )
        );

        redisTemplate.expire(key, Duration.ofMinutes(5));
    }

    public ProductData findById(Long id) {
        String key = "lab:product:" + id;

        Map<Object, Object> fields =
                redisTemplate.opsForHash().entries(key);

        if (fields.isEmpty()) {
            return null;
        }

        return new ProductData(
                (String) fields.get("name"),
                Integer.valueOf((String) fields.get("price"))
        );
    }

    public record ProductData(String name, Integer price) {}
}

