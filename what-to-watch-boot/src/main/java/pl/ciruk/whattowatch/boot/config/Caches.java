package pl.ciruk.whattowatch.boot.config;

import net.jodah.failsafe.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.cache.LongExpiry;
import pl.ciruk.whattowatch.boot.cache.RedisCache;
import pl.ciruk.whattowatch.boot.cache.ShortExpiry;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Optional;

@Configuration
public class Caches {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RedisProperties redis;
    private final W2wProperties.Cache cache;

    public Caches(RedisProperties redis, W2wProperties w2w) {
        this.redis = redis;
        this.cache = w2w.cache();
        LOGGER.info("{}", redis);
    }

    @Bean
    JedisPool redisConnectionPool() {
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redis.maxConnections());
        poolConfig.setMaxWait(Duration.ofMillis(500));
        poolConfig.setMinEvictableIdleDuration(Duration.ofMillis(100));
        return new JedisPool(poolConfig, redis.host(), Protocol.DEFAULT_PORT);
    }

    @Bean
    @LongExpiry
    CacheProvider<String> longExpiryCache(JedisPool jedisPool, CircuitBreaker<Optional<String>> circuitBreaker) {
        return new RedisCache(jedisPool, "long", cache.longExpiry(), circuitBreaker);
    }

    @Bean
    @ShortExpiry
    CacheProvider<String> shortExpiryCache(JedisPool jedisPool, CircuitBreaker<Optional<String>> circuitBreaker) {
        return new RedisCache(jedisPool, "short", cache.shortExpiry(), circuitBreaker);
    }

    @Bean
    CircuitBreaker<Optional<String>> circuitBreaker() {
        return new CircuitBreaker<Optional<String>>()
                .withFailureThreshold(3, 10)
                .withSuccessThreshold(5)
                .withDelay(Duration.ofSeconds(1));
    }

}
