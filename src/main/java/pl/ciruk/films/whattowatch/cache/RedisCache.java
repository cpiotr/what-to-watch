package pl.ciruk.films.whattowatch.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import pl.ciruk.core.cache.CacheProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

@Named
public class RedisCache implements CacheProvider<String> {

    private StringRedisTemplate cache;

    @Inject
    public RedisCache(StringRedisTemplate cache) {
        this.cache = cache;
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
