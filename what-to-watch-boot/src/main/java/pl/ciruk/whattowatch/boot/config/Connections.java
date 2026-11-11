package pl.ciruk.whattowatch.boot.config;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import jakarta.inject.Qualifier;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.cache.Cached;
import pl.ciruk.whattowatch.boot.cache.LongExpiry;
import pl.ciruk.whattowatch.boot.cache.NotCached;
import pl.ciruk.whattowatch.boot.cache.ShortExpiry;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.net.*;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

@Configuration
public class Connections {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final HttpProperties http;

    public Connections(HttpProperties http) {
        this.http = http;
        LOGGER.info("{}", http);
    }

    // Shared by both clients, so each domain has one limit
    @Bean
    DomainRateLimiter rateLimiter() {
        return new DomainRateLimiter(
                domain -> http.requestsPerSecondByDomain().getOrDefault(domain, http.requestsPerSecond()));
    }

    @Bean
    @AllCookies
    OkHttpClient httpClient() {
        return createHttpClientBuilder("HttpClient")
                .cookieJar(new InMemoryCookieJar())
                .build();
    }

    @Bean
    @NoCookies
    OkHttpClient noCookiesHttpClient() {
        return createHttpClientBuilder("HttpClient-NoCookies")
                .build();
    }

    private OkHttpClient.Builder createHttpClientBuilder(String name) {
        var connectionPool = new ConnectionPool(http.maxIdleConnections(), 30, TimeUnit.SECONDS);
        var metricsEventListener = OkHttpMetricsEventListener.builder(Metrics.globalRegistry, name).build();
        return new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(true)
                .readTimeout(2_000, TimeUnit.MILLISECONDS)
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .eventListener(metricsEventListener);
    }

    @Bean
    @NotCached
    @NoCookies
    HttpConnection<String> notCachedNoCookiesConnection(@NoCookies OkHttpClient httpClient, DomainRateLimiter rateLimiter) {
        return new HtmlConnection(httpClient, rateLimiter);
    }

    @Bean
    @NotCached
    @AllCookies
    HttpConnection<String> notCachedAllCookiesConnection(
            @AllCookies OkHttpClient httpClient,
            DomainRateLimiter rateLimiter) {
        return new HtmlConnection(httpClient, rateLimiter);
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
    HttpConnection<Element> longCachedJsoupConnection(@Cached @LongExpiry HttpConnection<String> connection) {
        return new JsoupConnection(connection);
    }

    @Bean
    @Cached
    @ShortExpiry
    HttpConnection<Element> shortCachedJsoupConnection(@Cached @ShortExpiry HttpConnection<String> connection) {
        return new JsoupConnection(connection);
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