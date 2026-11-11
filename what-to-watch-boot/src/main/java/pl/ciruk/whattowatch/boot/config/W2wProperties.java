package pl.ciruk.whattowatch.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties("w2w")
public record W2wProperties(
        @DefaultValue Pool pool,
        @DefaultValue Titles titles,
        @DefaultValue Suggestions suggestions,
        @DefaultValue Films films,
        @DefaultValue Cache cache,
        @DefaultValue Imdb imdb) {

    public record Pool(@DefaultValue("16") int size, @DefaultValue("false") boolean useVirtual) {
    }

    public record Titles(@DefaultValue("10") int pagesPerRequest) {
    }

    public record Suggestions(@DefaultValue("0.6") double scoreThreshold) {
    }

    public record Films(@DefaultValue("10000") int cacheSize) {
    }

    public record Cache(@DefaultValue("10d") Duration longExpiry, @DefaultValue("20m") Duration shortExpiry) {
    }

    // The Docker image downloads IMDb's datasets into /what-to-watch/imdb, its working directory
    public record Imdb(@DefaultValue("imdb") Path datasetsDirectory) {
    }
}
