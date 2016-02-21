package pl.ciruk.whattowatch.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.core.net.HttpConnection;
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
	HttpConnection<String> allCookiesConnection(OkHttpClient client) {
		new AllCookies().applyTo(client);
		return new HtmlConnection(client);
	}

	@Bean
	@Named("noCookies")
	HttpConnection<String> noCookiesConnection(OkHttpClient client) {
		return new HtmlConnection(client);
	}

	@Bean
	@Named("cachedConnection")
	HttpConnection<String> cachedConnection(CacheProvider<String> cacheProvider, @Named("noCookies") HttpConnection<String> connection) {
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
		return Executors.newFixedThreadPool(32, new ThreadFactoryBuilder().setNameFormat("films-pool-%d").build());
	}
}

