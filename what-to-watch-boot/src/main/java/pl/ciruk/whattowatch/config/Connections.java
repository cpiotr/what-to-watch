package pl.ciruk.whattowatch.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.AllCookies;
import pl.ciruk.core.net.CachedConnection;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.core.net.json.JsonConnection;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.util.function.Supplier;

@Configuration
@Slf4j
public class Connections {

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.pool.maxActive:8}")
    private Integer redisPoolMaxActive;

    @Bean
    Supplier<OkHttpClient> httpClientSupplier() {
        return OkHttpClient::new;
    }

    @Bean
    @Named("allCookies")
    HttpConnection<String> allCookiesConnection(Supplier<OkHttpClient> httpClientSupplier, MetricRegistry metricRegistry) {
        return new HtmlConnection(
                () -> {
                    OkHttpClient okHttpClient = httpClientSupplier.get();
                    new AllCookies().applyTo(okHttpClient);
                    return okHttpClient;
                },
                metricRegistry);
    }

    @Bean
    @Named("noCookies")
    HttpConnection<String> noCookiesConnection(Supplier<OkHttpClient> httpClientSupplier, MetricRegistry metricRegistry) {
        return new HtmlConnection(httpClientSupplier, metricRegistry);
    }

    @Bean
    @Named("cachedConnection")
    HttpConnection<String> cachedConnection(
            CacheProvider<String> cacheProvider,
            @Named("noCookies") HttpConnection<String> connection) {
        return new CachedConnection(cacheProvider, connection);
    }

    @Bean
    @Named("allCookiesHtml")
    HttpConnection<Element> jsoupConnectionAllCookies(@Named("allCookies") HttpConnection<String> allCookies) {
        return new JsoupConnection(allCookies);
    }

    @Bean
    @Named("noCookiesHtml")
    HttpConnection<Element> jsoupConnection(@Named("cachedConnection") HttpConnection<String> connection) {
        return new JsoupConnection(connection);
    }

    @Bean
    @Named("noCookiesJson")
    HttpConnection<JsonNode> jsonConnection(@Named("cachedConnection") HttpConnection<String> connection) {
        return new JsonConnection(connection);
    }

    @Bean
    @Primary
    StringRedisTemplate stringRedisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory());
    }

    private RedisConnectionFactory redisConnectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(redisHost);
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisPoolMaxActive);
        poolConfig.setMaxWaitMillis(1_000);
        poolConfig.setMinEvictableIdleTimeMillis(100);
        jedisConnectionFactory.setPoolConfig(poolConfig);
        jedisConnectionFactory.setShardInfo(new JedisShardInfo(redisHost));
        return jedisConnectionFactory;
    }

    @PostConstruct
    private void logConfiguration() {
        log.info("Redis host: <{}>", redisHost);
        log.info("Redis thread pool max active: <{}>", redisPoolMaxActive);
    }
}