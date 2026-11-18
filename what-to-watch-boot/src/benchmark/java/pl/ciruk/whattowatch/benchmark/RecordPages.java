package pl.ciruk.whattowatch.benchmark;

import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.OkHttpClient;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.utils.net.DomainRateLimiter;
import pl.ciruk.whattowatch.utils.net.OkHttpConnection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Asks the sites for the benchmark's titles once, at one request per second, and saves their answers to the file
 * given as the argument. Record again when a site changes its pages: {@code ./gradlew recordBenchmarkPages}.
 */
public final class RecordPages {
    private RecordPages() {
    }

    public static void main(String[] args) throws IOException {
        var sites = new RecordedPages.Recording(new OkHttpConnection(new OkHttpClient(), new DomainRateLimiter(domain -> 1)));
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            var suggestions = CachedScoringBenchmark.suggestionsFrom(sites, executorService, Caffeine.newBuilder().build());
            var scored = CompletableFutures.getAllOf(suggestions.suggest(TitleKind.FILM, 1)).toList();
            CachedScoringBenchmark.requireCriticAndAudienceScores(scored);
        }
        sites.save(Path.of(args[0]));
    }
}
