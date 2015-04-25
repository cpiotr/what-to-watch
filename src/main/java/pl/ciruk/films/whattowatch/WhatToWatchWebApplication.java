package pl.ciruk.films.whattowatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.films.whattowatch.cache.RedisCache;

import java.util.Optional;

@Configuration
@EnableAutoConfiguration
@ComponentScan("pl.ciruk")
@Slf4j
public class WhatToWatchWebApplication {

	@Bean
	CacheProvider<String> provideEmptyCache(StringRedisTemplate redis) {
        if (isRedisUpAndRunning(redis)) {
			return new RedisCache(redis);
		} else {
			return CacheProvider.empty();
		}
    }

    boolean isRedisUpAndRunning(RedisTemplate redisTemplate) {
        boolean closed;
        try {
            closed = Optional.ofNullable(redisTemplate)
                    .map(RedisTemplate::getConnectionFactory)
                    .map(RedisConnectionFactory::getConnection)
                    .map(RedisConnection::isClosed)
                    .orElse(true);
        } catch (Exception e) {
            closed = true;
        }
        return !closed;
    }

	public static void main(String[] args) {
		SpringApplication.run(WhatToWatchWebApplication.class, args);
	}
}
