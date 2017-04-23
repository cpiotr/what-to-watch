package pl.ciruk.whattowatch.cache;

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

    private final AtomicLong hitCounter = new AtomicLong();

    @Inject
    public RedisCache(StringRedisTemplate cache) {
        this.cache = cache;
    }

    @PostConstruct
    void init() {
        log.debug("init - RedisCache created");
    }

    @Override
    public Optional<String> get(String key) {
        Optional<String> optional = createGetCommand(key).execute();
        if (optional.isPresent()) {
            hitCounter.incrementAndGet();
        }
        return optional;
    }

    @Override
    public void put(String key, String value) {
        createPutCommand(key, value).execute();
    }

    private HystrixCommand<Optional<String>> createGetCommand(String key) {
        return new HystrixCommand<Optional<String>>(() -> "get") {
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
        return new HystrixCommand<Optional<Void>>(() -> "put") {
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

