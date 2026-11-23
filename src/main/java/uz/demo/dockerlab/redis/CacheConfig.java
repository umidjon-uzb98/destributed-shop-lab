package uz.demo.dockerlab.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import uz.demo.dockerlab.product.ProductView;
import uz.demo.dockerlab.product.ProductListView;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory
    ) {

        RedisCacheConfiguration productsConfig =
                createConfig(
                        ProductView.class,
                        Duration.ofMinutes(5)
                )
                        .entryTtl((key, value) ->
                                Duration.ofSeconds(
                                        ThreadLocalRandom.current()
                                                .nextLong(240, 301)
                                )
                        );
        ;

        RedisCacheConfiguration productListsConfig =
                createConfig(
                        ProductListView.class,
                        Duration.ofMinutes(2)
                );

        return RedisCacheManager
                .builder(connectionFactory)
                .withCacheConfiguration(
                        "products",
                        productsConfig
                )
                .withCacheConfiguration(
                        "product-lists",
                        productListsConfig
                )
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }

    private <T> RedisCacheConfiguration createConfig(
            Class<T> type,
            Duration ttl
    ) {

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(
                                        RedisSerializer.string()
                                )
                )
                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(
                                        new JacksonJsonRedisSerializer<>(type)
                                )
                );
    }
}
