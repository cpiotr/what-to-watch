package pl.ciruk.whattowatch.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("redis")
public record RedisProperties(@DefaultValue("localhost") String host, @DefaultValue("8") int maxConnections) {
}
