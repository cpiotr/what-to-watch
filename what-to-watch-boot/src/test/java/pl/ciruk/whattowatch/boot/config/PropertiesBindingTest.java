package pl.ciruk.whattowatch.boot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertiesBindingTest {

    @Test
    void shouldKeepDomainWithDotsAsOneKey() {
        var source = new MapConfigurationPropertySource(Map.of(
                "http.requests-per-second-by-domain.metacritic.com", "1.5",
                "http.requests-per-second", "4"));

        var http = new Binder(source).bind("http", HttpProperties.class).get();

        assertThat(http.requestsPerSecondByDomain()).containsExactly(Map.entry("metacritic.com", 1.5));
        assertThat(http.requestsPerSecond()).isEqualTo(4);
        assertThat(http.maxIdleConnections()).isEqualTo(64);
    }

    @Test
    void shouldLimitToTwoRequestsPerSecondByDefault() {
        var source = new MapConfigurationPropertySource(Map.of("http.max-idle-connections", "8"));

        var http = new Binder(source).bind("http", HttpProperties.class).get();

        assertThat(http.requestsPerSecond()).isEqualTo(2);
        assertThat(http.requestsPerSecondByDomain()).isEmpty();
    }

    @Test
    void shouldRejectRateThatIsNotPositive() {
        var source = new MapConfigurationPropertySource(Map.of("http.requests-per-second-by-domain.imdb.com", "0"));

        assertThatThrownBy(() -> new Binder(source).bind("http", HttpProperties.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .rootCause()
                .hasMessageContaining("http.requests-per-second-by-domain.imdb.com");
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
