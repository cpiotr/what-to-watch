package pl.ciruk.whattowatch.boot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesBindingTest {

    @Test
    void shouldKeepDomainWithDotsAsOneKey() {
        var source = new MapConfigurationPropertySource(Map.of(
                "http.delay-millis-by-domain.metacritic.com", "750",
                "http.default-delay-millis", "100"));

        var http = new Binder(source).bind("http", HttpProperties.class).get();

        assertThat(http.delayMillisByDomain()).containsExactly(Map.entry("metacritic.com", 750L));
        assertThat(http.defaultDelayMillis()).isEqualTo(100);
        assertThat(http.maxIdleConnections()).isEqualTo(64);
    }

    @Test
    void shouldUseDefaultsForMissingSettings() {
        var source = new MapConfigurationPropertySource(Map.of("w2w.cache.long-expiry", "10d"));

        var w2w = new Binder(source).bind("w2w", W2wProperties.class).get();

        assertThat(w2w.cache().longExpiry()).isEqualTo(Duration.ofDays(10));
        assertThat(w2w.cache().shortExpiry()).isEqualTo(Duration.ofMinutes(20));
        assertThat(w2w.pool().size()).isEqualTo(16);
        assertThat(w2w.suggestions().scoreThreshold()).isEqualTo(0.6);
    }
}
