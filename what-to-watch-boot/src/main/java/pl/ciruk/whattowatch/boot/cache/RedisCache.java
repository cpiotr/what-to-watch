package pl.ciruk.whattowatch.boot.cache;

import io.micrometer.core.instrument.Metrics;
import jakarta.annotation.PostConstruct;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.function.CheckedRunnable;
import net.jpountz.lz4.LZ4CompressorWithLength;
import net.jpountz.lz4.LZ4DecompressorWithLength;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.metrics.Names;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RedisCache implements CacheProvider<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_COMPRESSION_RATIO = 255;

    private final JedisPool jedisPool;
    private final AtomicLong missCounter = new AtomicLong();
    private final AtomicLong requestCounter = new AtomicLong();
    private final CircuitBreaker<Optional<String>> circuitBreaker;
    private final long expiryInterval;
    private final TimeUnit expiryUnit;
    private final LZ4Factory lz4Factory = LZ4Factory.fastestInstance();

    public RedisCache(
            JedisPool jedisPool,
            long expiryInterval,
            TimeUnit expiryUnit,
            CircuitBreaker<Optional<String>> circuitBreaker) {
        this.jedisPool = jedisPool;
        this.expiryInterval = expiryInterval;
        this.expiryUnit = expiryUnit;
        this.circuitBreaker = circuitBreaker;

        Metrics.gauge(
                Names.createName(RedisCache.class, List.of(expiryUnit, expiryInterval, "miss", "count")),
                missCounter,
                AtomicLong::get);
        Metrics.gauge(
                Names.createName(RedisCache.class, List.of(expiryUnit, expiryInterval, "request", "count")),
                requestCounter,
                AtomicLong::get);
    }

    @PostConstruct
    void init() {
        LOGGER.info("RedisCache created");
        LOGGER.info("Cache expiry: {} {}", expiryInterval, expiryUnit);
    }

    @Override
    public Optional<String> get(String key) {
        requestCounter.incrementAndGet();

        var optionalValue = Failsafe.with(circuitBreaker, Fallback.of(Optional.empty()))
                .onComplete(event -> {
                    if (event.getFailure() != null) {
                        LOGGER.error("Cannot retrieve value", event.getFailure());
                    }
                })
                .get(() -> getValueFromCache(key));
        if (optionalValue.isEmpty()) {
            LOGGER.debug("Missing key: {}", key);
            missCounter.incrementAndGet();
        }
        return optionalValue;
    }

    @Override
    public long removeAll(String keyExpression) {
        LOGGER.info("Remove all entries with keys matching: {}", keyExpression);

        try (Jedis jedis = jedisPool.getResource()) {
            final var keys = jedis.keys(keyExpression);
            return jedis.del(keys.toArray(String[]::new));
        }
    }

    private Optional<String> getValueFromCache(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            var keyBytes = key.getBytes(StandardCharsets.UTF_8);
            var compressed = jedis.get(keyBytes);
            if (compressed == null) {
                return Optional.empty();
            }
            var value = decompress(compressed);
            if (value.isEmpty()) {
                // An entry that can't be decompressed would never become readable, so make room for a fresh one
                jedis.del(keyBytes);
            }
            return value;
        }
    }

    private byte[] compress(String value) {
        return new LZ4CompressorWithLength(lz4Factory.fastCompressor())
                .compress(value.getBytes(StandardCharsets.UTF_8));
    }

    private Optional<String> decompress(byte[] compressed) {
        try {
            // A value that isn't LZ4 data can declare any length, so check it before allocating
            var length = LZ4DecompressorWithLength.getDecompressedLength(compressed);
            if (length < 0 || length > (long) compressed.length * MAX_COMPRESSION_RATIO) {
                LOGGER.warn("Unreadable cache entry of {} bytes", compressed.length);
                return Optional.empty();
            }
            var bytes = new LZ4DecompressorWithLength(lz4Factory.safeDecompressor()).decompress(compressed);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (LZ4Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException exception) {
            LOGGER.warn("Unreadable cache entry: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, String value) {
        Failsafe.with(circuitBreaker, Fallback.of(doNothing()))
                .run(() -> putValueToCache(key, value));
    }

    private void putValueToCache(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            var bytes = compress(value);
            jedis.setex(key.getBytes(StandardCharsets.UTF_8), getExpirySeconds(), bytes);
        }
    }

    private int getExpirySeconds() {
        return (int) expiryUnit.toSeconds(expiryInterval);
    }

    private CheckedRunnable doNothing() {
        return () -> {
        };
    }
}
