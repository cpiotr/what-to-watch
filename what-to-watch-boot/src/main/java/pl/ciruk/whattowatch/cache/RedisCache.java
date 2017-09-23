package pl.ciruk.whattowatch.cache;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.ciruk.core.cache.CacheProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Named
@Slf4j
public class RedisCache implements CacheProvider<String> {

    private final StringRedisTemplate cache;

    private final AtomicLong missCounter = new AtomicLong();

    private final AtomicLong requestCounter = new AtomicLong();

    private final CircuitBreaker circuitBreaker;

    @Inject
    public RedisCache(StringRedisTemplate cache, MetricRegistry metricRegistry) {
        this.cache = cache;

        metricRegistry.register(
                MetricRegistry.name(RedisCache.class, "missCounter"),
                (Gauge<Long>) missCounter::get);

        metricRegistry.register(
                MetricRegistry.name(RedisCache.class, "requestCounter"),
                (Gauge<Long>) requestCounter::get);

        circuitBreaker = new CircuitBreaker()
                .withFailureThreshold(3, 10)
                .withSuccessThreshold(5)
                .withDelay(1, TimeUnit.SECONDS)
                .withTimeout(5, TimeUnit.SECONDS);
    }

    @PostConstruct
    void init() {
        log.debug("init - RedisCache created");
    }

    @Override
    public Optional<String> get(String key) {
        requestCounter.incrementAndGet();

        Optional<String> optional = Failsafe.with(circuitBreaker)
                .withFallback(Optional.empty())
                .get(() -> getValueFromCache(key));
        if (!optional.isPresent()) {
            missCounter.incrementAndGet();
        }
        return optional;
    }

    private Optional<String> getValueFromCache(String key) {
        return Optional.ofNullable(cache.opsForValue().get(key));
    }

    @Override
    public void put(String key, String value) {
        Failsafe.with(circuitBreaker)
                .withFallback(() -> {})
                .run(() -> putValueToCache(key, value));
    }

    private void putValueToCache(String key, String value) {
        cache.opsForValue().set(key, value);
    }
}

