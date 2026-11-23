package uz.demo.dockerlab.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisMessageService {

    private final StringRedisTemplate redisTemplate;

    public RedisMessageService(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Long id, String message) {
        String key = "lab:message:" + id;

        redisTemplate.opsForValue()
                .set(key, message, Duration.ofSeconds(60));
    }

    public String findById(Long id) {
        String key = "lab:message:" + id;

        return redisTemplate.opsForValue().get(key);
    }

    public boolean delete(Long id) {
        String key = "lab:message:" + id;

        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public Long ttl(Long id) {
        String key = "lab:message:" + id;

        return redisTemplate.getExpire(key);
    }
}

