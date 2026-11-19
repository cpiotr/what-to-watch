package pl.ciruk.whattowatch.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties("w2w")
public record W2wProperties(
        @DefaultValue Titles titles,
        @DefaultValue Suggestions suggestions,
        @DefaultValue Films films,
        @DefaultValue Cache cache,
        @DefaultValue Imdb imdb,
        @DefaultValue Ratings ratings) {

    // Streaming services are JustWatch's short names: nfx is Netflix, prv is Amazon Prime Video, dnp is Disney+
    public record Titles(
            @DefaultValue("justwatch") TitleSource source,
            @DefaultValue("10") int pagesPerRequest,
            @DefaultValue({"nfx", "prv", "dnp"}) List<String> streamingServices) {
    }

    public enum TitleSource {
        JUSTWATCH,
        // Films only, so there are no series to suggest
        EKINO
    }

    // My predicted rating, from 1 to 10, that a film needs to be suggested
    public record Suggestions(@DefaultValue("5.8") double ratingThreshold) {
    }

    public record Films(@DefaultValue("10000") int cacheSize) {
    }

    public record Cache(@DefaultValue("10d") Duration longExpiry, @DefaultValue("1h") Duration shortExpiry) {
    }

    // The Docker image downloads IMDb's datasets into /what-to-watch/imdb, its working directory
    public record Imdb(@DefaultValue("imdb") Path datasetsDirectory) {
    }

    // My ratings, which the app writes. Docker Compose mounts the repository's ratings at /what-to-watch/ratings.
    public record Ratings(@DefaultValue("ratings/rated-films.csv") Path file) {
    }
}
