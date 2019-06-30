package pl.ciruk.whattowatch.boot.cache;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Metrics;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.function.CheckedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.metrics.Names;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static pl.ciruk.whattowatch.utils.stream.Functions.identity;

public class RedisCache implements CacheProvider<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final JedisPool jedisPool;
    private final AtomicLong missCounter = new AtomicLong();
    private final AtomicLong requestCounter = new AtomicLong();
    private final CircuitBreaker<Optional<String>> circuitBreaker;
    private final long expiryInterval;
    private final TimeUnit expiryUnit;

    public RedisCache(
            JedisPool jedisPool,
            long expiryInterval,
            TimeUnit expiryUnit,
            CircuitBreaker<Optional<String>> circuitBreaker) {
        this.jedisPool = jedisPool;
        this.expiryInterval = expiryInterval;
        this.expiryUnit = expiryUnit;
        this.circuitBreaker = circuitBreaker;

        Metrics.gauge(
                Names.createName(RedisCache.class, List.of(expiryUnit, expiryInterval, "miss", "count")),
                missCounter,
                AtomicLong::get);
        Metrics.gauge(
                Names.createName(RedisCache.class, List.of(expiryUnit, expiryInterval, "request", "count")),
                requestCounter,
                AtomicLong::get);
    }

    @PostConstruct
    void init() {
        LOGGER.info("RedisCache created");
        LOGGER.info("Cache expiry: {} {}", expiryInterval, expiryUnit);
    }

    @Override
    public Optional<String> get(String key) {
        requestCounter.incrementAndGet();

        var optionalValue = Failsafe.with(circuitBreaker, Fallback.of(Optional.empty()))
                .get(() -> getValueFromCache(key));
        if (optionalValue.isEmpty()) {
            LOGGER.debug("Missing key: {}", key);
            missCounter.incrementAndGet();
        }
        return optionalValue;
    }

    @SuppressFBWarnings(justification = "jedis")
    private Optional<String> getValueFromCache(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return Optional.ofNullable(jedis.get(key))
                    .map(identity(() -> jedis.expire(key, getExpirySeconds())));
        }
    }

    @Override
    public void put(String key, String value) {
        Failsafe.with(circuitBreaker, Fallback.of(doNothing()))
                .run(() -> putValueToCache(key, value));
    }

    @SuppressFBWarnings(justification = "jedis")
    private void putValueToCache(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
            jedis.expire(key, getExpirySeconds());
        }
    }

    private int getExpirySeconds() {
        return (int) expiryUnit.toSeconds(expiryInterval);
    }

    private CheckedRunnable doNothing() {
        return () -> {
        };
    }
}

