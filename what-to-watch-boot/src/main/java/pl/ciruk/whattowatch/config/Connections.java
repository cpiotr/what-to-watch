package pl.ciruk.whattowatch.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.jsoup.nodes.Element;
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
import pl.ciruk.core.net.json.JsonConnection;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class Connections {

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.pool.maxActive:8}")
    private Integer redisPoolMaxActive;

    @Value("${http.pool.maxIdle:32}")
    private Integer httpPoolMaxIdle;

    @Bean
    OkHttpClient httpClient() {
        ConnectionPool connectionPool = new ConnectionPool(httpPoolMaxIdle, 20_000, TimeUnit.SECONDS);
        return new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(true)
                .readTimeout(5_000, TimeUnit.MILLISECONDS)
                .connectTimeout(1_000, TimeUnit.MILLISECONDS)
                .build();
    }

    @Bean
    @Named("allCookies")
    HttpConnection<String> allCookiesConnection(OkHttpClient httpClient) {
        return new HtmlConnection(httpClient);
    }

    @Bean
    @Named("noCookies")
    HttpConnection<String> noCookiesConnection(OkHttpClient okHttpClient) {
        return new HtmlConnection(okHttpClient);
    }

    @Bean
    @Named("cachedConnection")
    HttpConnection<String> cachedConnection(
            @Named("redisCache") CacheProvider<String> cacheProvider,
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
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory());
    }

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisPoolMaxActive);
        poolConfig.setMaxWaitMillis(1_000);
        poolConfig.setMinEvictableIdleTimeMillis(100);
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder()
                .usePooling()
                .poolConfig(poolConfig)
                .build();

        return new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);
    }

    @PostConstruct
    private void logConfiguration() {
        log.info("Redis host: <{}>", redisHost);
        log.info("Redis thread pool max active: <{}>", redisPoolMaxActive);
        log.info("HttpClient pool max idle: <{}>", httpPoolMaxIdle);
    }
}