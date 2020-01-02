package pl.ciruk.whattowatch.boot.config;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import net.jodah.failsafe.CircuitBreaker;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.cache.*;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.net.*;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import javax.inject.Qualifier;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static pl.ciruk.whattowatch.boot.config.Configs.logConfigurationEntry;

@Configuration
@SuppressWarnings("PMD.TooManyMethods")
public class Connections {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String redisHost;
    private final Integer redisPoolMaxActive;
    private final Integer httpPoolMaxIdle;
    private final Integer httpConnectionFixedDelayInterval;
    private final TimeUnit httpConnectionFixedDelayUnit;
    private final long longExpiryInterval;
    private final TimeUnit longExpiryUnit;
    private final long shortExpiryInterval;
    private final TimeUnit shortExpiryUnit;
    private final Duration cloudflareDelay;

    public Connections(
            @Value("${redis.host}") String redisHost,
            @Value("${redis.pool.maxActive:8}") Integer redisPoolMaxActive,
            @Value("${http.pool.maxIdle:64}") Integer httpPoolMaxIdle,
            @Value("${http.connection.fixedDelay.interval:0}") Integer httpConnectionFixedDelayInterval,
            @Value("${http.connection.fixedDelay.unit:MILLISECONDS}") TimeUnit httpConnectionFixedDelayUnit,
            @Value("${w2w.cloudflare.wait.interval:5}") Integer cloudflareDelayInterval,
            @Value("${w2w.cloudflare.wait.unit:SECONDS}") TimeUnit cloudflareDelayUnit,
            @Value("${w2w.cache.expiry.long.interval:10}") long longExpiryInterval,
            @Value("${w2w.cache.expiry.long.unit:DAYS}") TimeUnit longExpiryUnit,
            @Value("${w2w.cache.expiry.short.interval:20}") long shortExpiryInterval,
            @Value("${w2w.cache.expiry.short.unit:MINUTES}") TimeUnit shortExpiryUnit) {
        this.redisHost = redisHost;
        this.redisPoolMaxActive = redisPoolMaxActive;
        this.httpPoolMaxIdle = httpPoolMaxIdle;
        this.httpConnectionFixedDelayInterval = httpConnectionFixedDelayInterval;
        this.httpConnectionFixedDelayUnit = httpConnectionFixedDelayUnit;
        this.longExpiryInterval = longExpiryInterval;
        this.longExpiryUnit = longExpiryUnit;
        this.shortExpiryInterval = shortExpiryInterval;
        this.shortExpiryUnit = shortExpiryUnit;

        this.cloudflareDelay = Duration.of(cloudflareDelayInterval, cloudflareDelayUnit.toChronoUnit());
    }

    @Bean
    BackoffInterceptor backoffInterceptor() {
        return new BackoffInterceptor(httpConnectionFixedDelayInterval, httpConnectionFixedDelayUnit);
    }

    @Bean
    @AllCookies
    OkHttpClient httpClient(BackoffInterceptor backoffInterceptor) {
        var connectionPool = new ConnectionPool(httpPoolMaxIdle, 20, TimeUnit.SECONDS);
        var metricsEventListener = OkHttpMetricsEventListener.builder(Metrics.globalRegistry, "HttpClient").build();
        return new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(true)
                .readTimeout(2_000, TimeUnit.MILLISECONDS)
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .eventListener(metricsEventListener)
                .addInterceptor(backoffInterceptor)
                .cookieJar(new InMemoryCookieJar())
                .build();
    }

    @Bean
    @NoCookies
    OkHttpClient noCookiesHttpClient(BackoffInterceptor backoffInterceptor) {
        var connectionPool = new ConnectionPool(httpPoolMaxIdle, 20, TimeUnit.SECONDS);
        var metricsEventListener = OkHttpMetricsEventListener.builder(Metrics.globalRegistry, "HttpClient").build();
        return new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(true)
                .readTimeout(2_000, TimeUnit.MILLISECONDS)
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .eventListener(metricsEventListener)
                .addInterceptor(backoffInterceptor)
                .build();
    }

    @Bean
    ScriptEngine engine() {
        return new ScriptEngineManager().getEngineByName("js");
    }

    @Bean
    JavascriptChallengeSolver challengeSolver(ScriptEngine engine) {
        return new JavascriptChallengeSolver(engine);
    }

    @Bean
    ResponseProcessor cloudflareBypass(@AllCookies OkHttpClient httpClient, ScriptEngine engine) {
        return new CloudflareBypass(httpClient, engine, cloudflareDelay);
    }

    @Bean
    @NotCached
    @NoCookies
    HttpConnection<String> notCachedNoCookiesConnection(@NoCookies OkHttpClient httpClient) {
        return new HtmlConnection(httpClient);
    }

    @Bean
    @NotCached
    @AllCookies
    HttpConnection<String> notCachedConnection(
            @AllCookies OkHttpClient httpClient,
            List<RequestProcessor> requestProcessors,
            List<ResponseProcessor> responseProcessors) {
        logConfigurationEntry(LOGGER, "Request processors", requestProcessors);
        logConfigurationEntry(LOGGER, "Response processors", responseProcessors);
        return new HtmlConnection(httpClient, requestProcessors, responseProcessors);
    }

    @Bean
    @Cached
    @LongExpiry
    HttpConnection<String> longCachedConnection(
            @LongExpiry CacheProvider<String> cacheProvider,
            @NotCached @NoCookies HttpConnection<String> connection) {
        return new CachedConnection(cacheProvider, connection);
    }

    @Bean
    @Cached
    @ShortExpiry
    HttpConnection<String> shortCachedConnection(
            @ShortExpiry CacheProvider<String> cacheProvider,
            @NotCached @AllCookies HttpConnection<String> connection) {
        return new CachedConnection(cacheProvider, connection);
    }

    @Bean
    @Cached
    @LongExpiry
    HttpConnection<Element> jsoupConnection(@Cached @LongExpiry HttpConnection<String> connection) {
        return new JsoupConnection(connection);
    }

    @Bean
    @Cached
    @ShortExpiry
    HttpConnection<Element> notCachedJsoupConnection(@Cached @ShortExpiry HttpConnection<String> connection) {
        return new JsoupConnection(connection);
    }

    @Bean
    JedisPool redisConnectionPool() {
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisPoolMaxActive);
        poolConfig.setMaxWaitMillis(500);
        poolConfig.setMinEvictableIdleTimeMillis(100);
        return new JedisPool(poolConfig, redisHost);
    }

    @Bean
    @LongExpiry
    CacheProvider<String> longExpiryCache(JedisPool jedisPool, CircuitBreaker<Optional<String>> circuitBreaker) {
        return new RedisCache(jedisPool, longExpiryInterval, longExpiryUnit, circuitBreaker);
    }

    @Bean
    @ShortExpiry
    CacheProvider<String> shortExpiryCache(JedisPool jedisPool, CircuitBreaker<Optional<String>> circuitBreaker) {
        return new RedisCache(jedisPool, shortExpiryInterval, shortExpiryUnit, circuitBreaker);
    }

    @Bean
    CircuitBreaker<Optional<String>> circuitBreaker() {
        return new CircuitBreaker<Optional<String>>()
                .withFailureThreshold(3, 10)
                .withSuccessThreshold(5)
                .withDelay(Duration.ofSeconds(1))
                .withTimeout(Duration.ofSeconds(5));
    }

    @PostConstruct
    void logConfiguration() {
        logConfigurationEntry(LOGGER, "Redis host", redisHost);
        logConfigurationEntry(LOGGER, "Redis thread pool max active", redisPoolMaxActive);
        logConfigurationEntry(LOGGER, "Cache long expiry", longExpiryInterval, longExpiryUnit);
        logConfigurationEntry(LOGGER, "Cache short expiry", shortExpiryInterval, shortExpiryUnit);
        logConfigurationEntry(LOGGER, "HttpClient pool max idle", httpPoolMaxIdle);
        logConfigurationEntry(LOGGER, "HttpClient fixed delay", httpConnectionFixedDelayInterval, httpConnectionFixedDelayUnit);
        logConfigurationEntry(LOGGER, "Cloudflare delay", cloudflareDelay);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @Qualifier
    @interface AllCookies {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @Qualifier
    @interface NoCookies {
    }
}