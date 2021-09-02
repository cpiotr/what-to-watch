package pl.ciruk.whattowatch.boot.cache;

import net.jodah.failsafe.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisCacheTest {
    @Container
    public GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());
    private RedisCache redisCache;
    private JedisPool jedisPool;

    @BeforeEach
    void setUp() {
        jedisPool = new JedisPool(redis.getHost(), redis.getFirstMappedPort());
        redisCache = new RedisCache(jedisPool, 1, TimeUnit.HOURS, new CircuitBreaker<>());
        try (var jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    @Test
    void shouldRetrieveElementFromCache() {
        redisCache.put("TestKey", "TestValue");

        assertThat(redisCache.get("TestKey")).hasValue("TestValue");
    }

    @Test
    void shouldReadStringValueInsertedDirectly() {
        var key = "TestKey";
        var value = "TestValue";
        try (var jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        }

        assertThat(redisCache.get(key)).hasValue(value);
    }

    @Disabled
    @Test
    void shouldReplaceDirectStringValueWithCompressedOption() {
        var key = "TestKey";
        var value = "TestValueTestValueTestValueTestValueTestValueTestValueTestValue";
        try (var jedis = jedisPool.getResource()) {
            jedis.set(key, value);

            redisCache.get(key);

            var bytes = jedis.get(key.getBytes(StandardCharsets.UTF_8));
            assertThat(bytes).hasSizeLessThan(value.getBytes(StandardCharsets.UTF_8).length);
        }

        assertThat(redisCache.get(key)).hasValue(value);
    }
}
