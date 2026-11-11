package pl.ciruk.whattowatch.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

// Rates are plain numbers: Spring keeps dotted map keys such as "metacritic.com" whole only for simple value types
@ConfigurationProperties("http")
public record HttpProperties(
        @DefaultValue("64") int maxIdleConnections,
        @DefaultValue("2") double requestsPerSecond,
        Map<String, Double> requestsPerSecondByDomain) {

    public HttpProperties {
        requestsPerSecondByDomain = requestsPerSecondByDomain == null ? Map.of() : Map.copyOf(requestsPerSecondByDomain);
        requirePositive("http.requests-per-second", requestsPerSecond);
        requestsPerSecondByDomain.forEach(
                (domain, rate) -> requirePositive("http.requests-per-second-by-domain." + domain, rate));
    }

    private static void requirePositive(String name, double rate) {
        if (rate <= 0) {
            throw new IllegalArgumentException(name + " must be positive, but is " + rate);
        }
    }
}
