package pl.ciruk.whattowatch.cache;

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
        Optional<String> optional = Optional.ofNullable(cache.opsForValue().get(key));
        if (optional.isPresent()) {
            hitCounter.incrementAndGet();
        }
        return optional;
    }

    @Override
    public void put(String key, String value) {
        cache.opsForValue().set(key, value);
    }
}

