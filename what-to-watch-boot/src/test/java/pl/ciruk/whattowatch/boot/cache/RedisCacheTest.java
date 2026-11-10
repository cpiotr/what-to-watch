package pl.ciruk.whattowatch.boot.cache;

import net.jodah.failsafe.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisCacheTest {
    static final String TEST_VALUE = "TestValue";
    static final String TEST_KEY = "TestKey";
    @Container
    private final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:6.2.24-alpine"))
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
        redisCache.put(TEST_KEY, TEST_VALUE);

        assertThat(redisCache.get(TEST_KEY)).hasValue(TEST_VALUE);
    }

    @Test
    void shouldRetrieveValueLargerThanTwoMegabytes() {
        var value = "<p>Long page</p>".repeat(200_000);

        redisCache.put(TEST_KEY, value);

        assertThat(redisCache.get(TEST_KEY)).hasValue(value);
    }

    @Test
    void shouldRemoveEntryThatCannotBeRead() {
        try (var jedis = jedisPool.getResource()) {
            jedis.set(TEST_KEY, TEST_VALUE);

            assertThat(redisCache.get(TEST_KEY)).isEmpty();
            assertThat(jedis.exists(TEST_KEY)).isFalse();
        }
    }

    @Test
    void shouldNotExtendExpiryWhenRead() {
        redisCache.put(TEST_KEY, TEST_VALUE);
        try (var jedis = jedisPool.getResource()) {
            jedis.expire(TEST_KEY, 100);

            redisCache.get(TEST_KEY);

            assertThat(jedis.ttl(TEST_KEY)).isLessThanOrEqualTo(100);
        }
    }

    @Test
    void shouldRemoveEntriesWithKeysMatchingGivenExpression() {
        redisCache.put(TEST_KEY, TEST_VALUE);
        var expectedCount = 100;
        for (var i = 0; i < expectedCount; i ++) {
            redisCache.put(TEST_KEY + "suffix" + i, TEST_VALUE);
        }

        assertThat(redisCache.removeAll(TEST_KEY + "suffix" + "*")).isEqualTo(100L);
        assertThat(redisCache.removeAll(TEST_KEY)).isEqualTo(1L);
    }
}
