package pl.ciruk.whattowatch.boot.config;

import net.jodah.failsafe.CircuitBreaker;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.CachedConnection;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.boot.cache.*;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

@Configuration
public class Connections {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.pool.maxActive:8}")
    private Integer redisPoolMaxActive;

    @Value("${http.pool.maxIdle:32}")
    private Integer httpPoolMaxIdle;

    @Value("${w2w.cache.expiry.interval}")
    private long expiryInterval = 10;

    @Value("${w2w.cache.expiry.unit}")
    private TimeUnit expiryUnit = TimeUnit.DAYS;

    @Bean
    OkHttpClient httpClient() {
        var connectionPool = new ConnectionPool(httpPoolMaxIdle, 20_000, TimeUnit.SECONDS);
        return new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(true)
                .readTimeout(5_000, TimeUnit.MILLISECONDS)
                .connectTimeout(1_000, TimeUnit.MILLISECONDS)
                .build();
    }

    @Bean
    @NotCached
    HttpConnection<String> notCachedConnection(OkHttpClient httpClient) {
        return new HtmlConnection(httpClient);
    }

    @Bean
    @Cached
    @LongExpiry
    HttpConnection<String> longCachedConnection(@LongExpiry CacheProvider<String> cacheProvider, @NotCached HttpConnection<String> connection) {
        return new CachedConnection(cacheProvider, connection);
    }

    @Bean
    @Cached
    @ShortExpiry
    HttpConnection<String> shortCachedConnection(@ShortExpiry CacheProvider<String> cacheProvider, @NotCached HttpConnection<String> connection) {
        return new CachedConnection(cacheProvider, connection);
    }

    @Bean
    @Cached
    HttpConnection<Element> jsoupConnection(@Cached @LongExpiry HttpConnection<String> connection) {
        return new JsoupConnection(connection);
    }

    @Bean
    @NotCached
    HttpConnection<Element> notCachedJsoupConnection(@Cached @ShortExpiry HttpConnection<String> connection) {
        return new JsoupConnection(connection);
    }

    @Bean
    @Primary
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory());
    }

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost);

        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisPoolMaxActive);
        poolConfig.setMaxWaitMillis(1_000);
        poolConfig.setMinEvictableIdleTimeMillis(100);
        var jedisClientConfiguration = JedisClientConfiguration.builder()
                .usePooling()
                .poolConfig(poolConfig)
                .build();

        return new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);
    }

    @Bean
    @LongExpiry
    CacheProvider<String> longExpiryCache(StringRedisTemplate redisTemplate, CircuitBreaker circuitBreaker) {
        return new RedisCache(redisTemplate, expiryInterval, expiryUnit, circuitBreaker);
    }

    @Bean
    @ShortExpiry
    CacheProvider<String> shortExpiryCache(StringRedisTemplate redisTemplate, CircuitBreaker circuitBreaker) {
        return new RedisCache(redisTemplate, 15, TimeUnit.MINUTES, circuitBreaker);
    }

    @Bean
    CircuitBreaker circuitBreaker() {
        return new CircuitBreaker()
                .withFailureThreshold(3, 10)
                .withSuccessThreshold(5)
                .withDelay(1, TimeUnit.SECONDS)
                .withTimeout(5, TimeUnit.SECONDS);
    }

    @PostConstruct
    private void logConfiguration() {
        LOGGER.info("Redis host: <{}>", redisHost);
        LOGGER.info("Redis thread pool max active: <{}>", redisPoolMaxActive);
        LOGGER.info("Cache expiry: <{} {}>", expiryInterval, expiryUnit);
        LOGGER.info("HttpClient pool max idle: <{}>", httpPoolMaxIdle);
    }
}