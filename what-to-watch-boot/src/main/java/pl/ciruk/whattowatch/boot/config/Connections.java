package pl.ciruk.whattowatch.boot.config;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Qualifier;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static pl.ciruk.whattowatch.boot.config.Configs.logConfigurationEntry;

@Configuration
@SuppressWarnings("PMD.TooManyMethods")
public class Connections {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Integer httpPoolMaxIdle;
    private final Long httpConnectionDefaultDelay;
    private final Map<String, Long> httpConnectionDelayByDomain;
    private final TimeUnit httpConnectionDelayByDomainUnit;

    public Connections(
            @Value("${http.pool.maxIdle:64}") Integer httpPoolMaxIdle,
            @Value("${http.connection.delayByDomain.default:0}") Long httpConnectionDefaultDelay,
            @Value("#{${http.connection.delayByDomain.map:null}}") Map<String, Long> httpConnectionDelayByDomain,
            @Value("${http.connection.delayByDomain.unit:MILLISECONDS}") TimeUnit httpConnectionDelayByDomainUnit) {
        this.httpPoolMaxIdle = httpPoolMaxIdle;
        this.httpConnectionDefaultDelay = httpConnectionDefaultDelay;
        this.httpConnectionDelayByDomain = httpConnectionDelayByDomain == null
                ? new HashMap<>()
                : httpConnectionDelayByDomain;
        this.httpConnectionDelayByDomainUnit = httpConnectionDelayByDomainUnit;
    }

    @Bean
    BackoffInterceptor backoffInterceptor() {
        Function<String, Duration> durationByDomain = domain -> {
            var delay = httpConnectionDelayByDomain.getOrDefault(domain, httpConnectionDefaultDelay);
            return Duration.of(delay, httpConnectionDelayByDomainUnit.toChronoUnit());
        };

        return new BackoffInterceptor(durationByDomain);
    }

    @Bean
    @AllCookies
    OkHttpClient httpClient(BackoffInterceptor backoffInterceptor) {
        return createHttpClientBuilder("HttpClient", backoffInterceptor)
                .cookieJar(new InMemoryCookieJar())
                .build();
    }

    @Bean
    @NoCookies
    OkHttpClient noCookiesHttpClient(BackoffInterceptor backoffInterceptor) {
        return createHttpClientBuilder("HttpClient-NoCookies", backoffInterceptor)
                .build();
    }

    private OkHttpClient.Builder createHttpClientBuilder(String name, BackoffInterceptor backoffInterceptor) {
        var connectionPool = new ConnectionPool(httpPoolMaxIdle, 30, TimeUnit.SECONDS);
        var metricsEventListener = OkHttpMetricsEventListener.builder(Metrics.globalRegistry, name).build();
        return new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(true)
                .readTimeout(2_000, TimeUnit.MILLISECONDS)
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .eventListener(metricsEventListener)
                .addInterceptor(backoffInterceptor);
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

    @PostConstruct
    void logConfiguration() {
        logConfigurationEntry(LOGGER, "HttpClient pool max idle", httpPoolMaxIdle);
        logConfigurationEntry(LOGGER, "HttpClient default delay", httpConnectionDefaultDelay);
        logConfigurationEntry(LOGGER, "HttpClient delay by domain", httpConnectionDelayByDomain);
        logConfigurationEntry(LOGGER, "HttpClient delay unit", httpConnectionDelayByDomainUnit);
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