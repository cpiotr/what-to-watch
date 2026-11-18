package pl.ciruk.whattowatch.boot.config;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.net.CachedConnection;
import pl.ciruk.whattowatch.utils.net.DomainRateLimiter;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.utils.net.OkHttpConnection;

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

    // Shared by every connection, so each domain has one limit
    @Bean
    DomainRateLimiter rateLimiter() {
        return new DomainRateLimiter(
                domain -> http.requestsPerSecondByDomain().getOrDefault(domain, http.requestsPerSecond()));
    }

    @Bean
    OkHttpClient httpClient() {
        var connectionPool = new ConnectionPool(http.maxIdleConnections(), 30, TimeUnit.SECONDS);
        var metricsEventListener = OkHttpMetricsEventListener.builder(Metrics.globalRegistry, "HttpClient-NoCookies").build();
        return new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(true)
                .readTimeout(2_000, TimeUnit.MILLISECONDS)
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .eventListener(metricsEventListener)
                .build();
    }

    // Filmweb's and Metacritic's pages, kept in Redis for w2w.cache.long-expiry
    @Bean
    HttpConnection<String> cachedPages(
            CacheProvider<String> pageCache,
            OkHttpClient httpClient,
            DomainRateLimiter rateLimiter) {
        return new CachedConnection(pageCache, new OkHttpConnection(httpClient, rateLimiter));
    }
}
