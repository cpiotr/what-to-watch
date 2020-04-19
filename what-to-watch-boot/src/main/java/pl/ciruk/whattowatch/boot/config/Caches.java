package pl.ciruk.whattowatch.boot.config;

import net.jodah.failsafe.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.cache.LongExpiry;
import pl.ciruk.whattowatch.boot.cache.RedisCache;
import pl.ciruk.whattowatch.boot.cache.ShortExpiry;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static pl.ciruk.whattowatch.boot.config.Configs.logConfigurationEntry;

@Configuration
@SuppressWarnings("PMD.TooManyMethods")
public class Caches {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String redisHost;
    private final Integer redisPoolMaxActive;
    private final long longExpiryInterval;
    private final TimeUnit longExpiryUnit;
    private final long shortExpiryInterval;
    private final TimeUnit shortExpiryUnit;

    public Caches(
            @Value("${redis.host}") String redisHost,
            @Value("${redis.pool.maxActive:8}") Integer redisPoolMaxActive,
            @Value("${w2w.cache.expiry.long.interval:10}") long longExpiryInterval,
            @Value("${w2w.cache.expiry.long.unit:DAYS}") TimeUnit longExpiryUnit,
            @Value("${w2w.cache.expiry.short.interval:20}") long shortExpiryInterval,
            @Value("${w2w.cache.expiry.short.unit:MINUTES}") TimeUnit shortExpiryUnit) {
        this.redisHost = redisHost;
        this.redisPoolMaxActive = redisPoolMaxActive;
        this.longExpiryInterval = longExpiryInterval;
        this.longExpiryUnit = longExpiryUnit;
        this.shortExpiryInterval = shortExpiryInterval;
        this.shortExpiryUnit = shortExpiryUnit;
    }

    @Bean
    JedisPool redisConnectionPool() {
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisPoolMaxActive);
        poolConfig.setMaxWaitMillis(500);
        poolConfig.setMinEvictableIdleTimeMillis(100);
        return new JedisPool(poolConfig, redisHost);
    }

    @Bean
    @LongExpiry
    CacheProvider<String> longExpiryCache(JedisPool jedisPool, CircuitBreaker<Optional<String>> circuitBreaker) {
        return new RedisCache(jedisPool, longExpiryInterval, longExpiryUnit, circuitBreaker);
    }

    @Bean
    @ShortExpiry
    CacheProvider<String> shortExpiryCache(JedisPool jedisPool, CircuitBreaker<Optional<String>> circuitBreaker) {
        return new RedisCache(jedisPool, shortExpiryInterval, shortExpiryUnit, circuitBreaker);
    }

    @Bean
    CircuitBreaker<Optional<String>> circuitBreaker() {
        return new CircuitBreaker<Optional<String>>()
                .withFailureThreshold(3, 10)
                .withSuccessThreshold(5)
                .withDelay(Duration.ofSeconds(1));
    }

    @PostConstruct
    void logConfiguration() {
        logConfigurationEntry(LOGGER, "Redis host", redisHost);
        logConfigurationEntry(LOGGER, "Redis thread pool max active", redisPoolMaxActive);
        logConfigurationEntry(LOGGER, "Cache long expiry", longExpiryInterval, longExpiryUnit);
        logConfigurationEntry(LOGGER, "Cache short expiry", shortExpiryInterval, shortExpiryUnit);
    }
}