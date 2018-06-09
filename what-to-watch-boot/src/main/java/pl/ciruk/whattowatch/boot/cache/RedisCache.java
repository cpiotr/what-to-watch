package pl.ciruk.whattowatch.boot.cache;

import io.micrometer.core.instrument.Metrics;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.function.CheckedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static pl.ciruk.whattowatch.utils.stream.Functions.identity;

public class RedisCache implements CacheProvider<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final StringRedisTemplate cache;

    private final AtomicLong missCounter = new AtomicLong();

    private final AtomicLong requestCounter = new AtomicLong();

    private final CircuitBreaker circuitBreaker;

    private final long expiryInterval;

    private final TimeUnit expiryUnit;

    public RedisCache(StringRedisTemplate cache, long expiryInterval, TimeUnit expiryUnit, CircuitBreaker circuitBreaker) {
        String className = MethodHandles.lookup().lookupClass().getSimpleName();
        Metrics.gauge(
                String.format("%s.%s%s.%s", className, expiryInterval, expiryUnit, "missCounter"),
                missCounter,
                AtomicLong::get);
        Metrics.gauge(
                String.format("%s.%s%s.%s", className, expiryInterval, expiryUnit, "requestCounter"),
                requestCounter,
                AtomicLong::get);

        this.cache = cache;
        this.expiryInterval = expiryInterval;
        this.expiryUnit = expiryUnit;
        this.circuitBreaker = circuitBreaker;
    }

    @PostConstruct
    private void init() {
        LOGGER.info("init - RedisCache created");
        LOGGER.info("init - Cache expiry: {} {}", expiryInterval, expiryUnit);
    }

    @Override
    public Optional<String> get(String key) {
        requestCounter.incrementAndGet();

        var optionalValue = Failsafe.with(circuitBreaker)
                .withFallback(Optional.empty())
                .get(() -> getValueFromCache(key));
        if (!optionalValue.isPresent()) {
            missCounter.incrementAndGet();
        }
        return optionalValue;
    }

    private Optional<String> getValueFromCache(String key) {
        return Optional.ofNullable(cache.opsForValue().get(key))
                .map(identity(() -> cache.expire(key, expiryInterval, expiryUnit)));
    }

    @Override
    public void put(String key, String value) {
        Failsafe.with(circuitBreaker)
                .withFallback(doNothing())
                .run(() -> putValueToCache(key, value));
    }

    private void putValueToCache(String key, String value) {
        cache.opsForValue().set(key, value);
        cache.expire(key, expiryInterval, expiryUnit);
    }

    private CheckedRunnable doNothing() {
        return () -> {
        };
    }
}
