package pl.ciruk.whattowatch.cache;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.netflix.hystrix.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.ciruk.core.cache.CacheProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Named
@Slf4j
public class RedisCache implements CacheProvider<String> {

    private final StringRedisTemplate cache;

    private final AtomicLong missCounter = new AtomicLong();

    private final AtomicLong requestCounter = new AtomicLong();

    @Inject
    public RedisCache(StringRedisTemplate cache, MetricRegistry metricRegistry) {
        this.cache = cache;

        metricRegistry.register(
                MetricRegistry.name(RedisCache.class, "missCounter"),
                (Gauge<Long>) missCounter::get);

            metricRegistry.register(
                    MetricRegistry.name(RedisCache.class, "requestCounter"),
                    (Gauge<Long>) requestCounter::get);
    }

    @PostConstruct
    void init() {
        log.debug("init - RedisCache created");
    }

    @Override
    public Optional<String> get(String key) {
        requestCounter.incrementAndGet();

        Optional<String> optional = createGetCommand(key).execute();
        if (!optional.isPresent()) {
            missCounter.incrementAndGet();
        }
        return optional;
    }

    @Override
    public void put(String key, String value) {
        createPutCommand(key, value).execute();
    }

    private HystrixCommand<Optional<String>> createGetCommand(String key) {
        return new HystrixCommand<Optional<String>>(() -> "RedisCache.get") {
            @Override
            protected Optional<String> run() throws Exception {
                return Optional.ofNullable(
                        cache.opsForValue().get(key));
            }

            @Override
            protected Optional<String> getFallback() {
                return Optional.empty();
            }
        };
    }

    private HystrixCommand<Optional<Void>> createPutCommand(String key, String value) {
        return new HystrixCommand<Optional<Void>>(() -> "RedisCache.put") {
            @Override
            protected Optional<Void> run() throws Exception {
                cache.opsForValue().set(key, value);
                return Optional.empty();
            }

            @Override
            protected Optional<Void> getFallback() {
                return Optional.empty();
            }
        };
    }
}

