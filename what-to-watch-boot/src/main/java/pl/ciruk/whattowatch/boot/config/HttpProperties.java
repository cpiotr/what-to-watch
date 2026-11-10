package pl.ciruk.whattowatch.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

// Delays are plain millis: Spring keeps dotted map keys such as "metacritic.com" whole only for simple value types
@ConfigurationProperties("http")
public record HttpProperties(
        @DefaultValue("64") int maxIdleConnections,
        @DefaultValue("0") long defaultDelayMillis,
        Map<String, Long> delayMillisByDomain) {

    public HttpProperties {
        delayMillisByDomain = delayMillisByDomain == null ? Map.of() : Map.copyOf(delayMillisByDomain);
    }
}
