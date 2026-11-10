package pl.ciruk.whattowatch.boot.cache;

import net.jodah.failsafe.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.RedisClient;

import java.time.Duration;

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
    private RedisClient redisClient;

    @BeforeEach
    void setUp() {
        redisClient = RedisClient.create(redis.getHost(), redis.getFirstMappedPort());
        redisCache = new RedisCache(redisClient, "test", Duration.ofHours(1), new CircuitBreaker<>());
        redisClient.flushAll();
    }

    @AfterEach
    void tearDown() {
        redisClient.close();
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
        redisClient.set(TEST_KEY, TEST_VALUE);

        assertThat(redisCache.get(TEST_KEY)).isEmpty();
        assertThat(redisClient.exists(TEST_KEY)).isFalse();
    }

    @Test
    void shouldNotExtendExpiryWhenRead() {
        redisCache.put(TEST_KEY, TEST_VALUE);
        redisClient.expire(TEST_KEY, 100);

        redisCache.get(TEST_KEY);

        assertThat(redisClient.ttl(TEST_KEY)).isLessThanOrEqualTo(100);
    }
}
