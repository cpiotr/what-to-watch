package pl.ciruk.whattowatch.config;

import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.AllCookies;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.core.net.JsoupConnection;
import redis.clients.jedis.JedisShardInfo;

import javax.inject.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Slf4j
public class Application {

	@Bean
	OkHttpClient httpClient() {
		OkHttpClient httpClient = new OkHttpClient();
		return httpClient;
	}

	@Bean
	@Named("allCookies")
	JsoupConnection jsoupConnectionAllCookies(CacheProvider<String> cacheProvider, OkHttpClient httpClient) {
		new AllCookies().applyTo(httpClient);
		return new JsoupCachedConnection(cacheProvider, httpClient);
	}

	@Bean
	JsoupConnection jsoupConnection(CacheProvider<String> cacheProvider, OkHttpClient httpClient) {
		return new JsoupCachedConnection(cacheProvider, httpClient);
	}

	@Bean
	@Primary
	StringRedisTemplate stringRedisTemplate() {
		RedisConnectionFactory connectionFactory = redisConnectionFactory();
		log.debug("stringRedisTemplate - host: {}", redisHost);
		return new StringRedisTemplate(connectionFactory);
	}

	@Value("${redis.host}")
	String redisHost;

	private RedisConnectionFactory redisConnectionFactory() {
		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
		jedisConnectionFactory.setHostName(redisHost);
		jedisConnectionFactory.setShardInfo(new JedisShardInfo(redisHost));
		return jedisConnectionFactory;
	}

	@Bean
	ExecutorService executorService() {
		return Executors.newFixedThreadPool(32);
	}
}

