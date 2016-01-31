package pl.ciruk.whattowatch.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.ciruk.core.cache.CacheProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

@Named
@Slf4j
public class RedisCache implements CacheProvider<String> {

    private StringRedisTemplate cache;

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
        String value = cache.opsForValue().get(key);
        return Optional.ofNullable(value);
    }

    @Override
    public void put(String key, String value) {
        cache.opsForValue().set(key, value);
    }
}
