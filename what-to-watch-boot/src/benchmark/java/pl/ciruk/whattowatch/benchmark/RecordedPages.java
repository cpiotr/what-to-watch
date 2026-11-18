package pl.ciruk.whattowatch.benchmark;

import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The sites' answers for the benchmark's titles, by URL, so the benchmark runs without asking the sites. A page that
 * wasn't recorded is missing, as a page the site doesn't have.
 */
final class RecordedPages implements HttpConnection<String> {
    static final String RESOURCE = "recorded-pages.json.gz";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Map<String, String> pagesByUrl;

    private RecordedPages(Map<String, String> pagesByUrl) {
        this.pagesByUrl = pagesByUrl;
    }

    static RecordedPages load() {
        try (InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (resource == null) {
                throw new IllegalStateException("Missing " + RESOURCE + ": record it with ./gradlew recordBenchmarkPages");
            }
            try (var gzip = new GZIPInputStream(resource)) {
                Map<String, String> pagesByUrl = JSON.readValue(gzip, new TypeReference<>() {
                });
                return new RecordedPages(pagesByUrl);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<String> get(HttpUrl url) {
        return Optional.ofNullable(pagesByUrl.get(url.toString()));
    }

    @Override
    public Optional<String> post(HttpUrl url, RequestBody body) {
        throw new UnsupportedOperationException("Only pages that are read with GET are recorded");
    }

    // Passes requests to the sites and keeps what they answer
    static final class Recording implements HttpConnection<String> {
        private final HttpConnection<String> sites;
        private final Map<String, String> pagesByUrl = new ConcurrentHashMap<>();

        Recording(HttpConnection<String> sites) {
            this.sites = sites;
        }

        @Override
        public Optional<String> get(HttpUrl url) {
            var page = sites.get(url);
            page.ifPresent(found -> pagesByUrl.put(url.toString(), found));
            return page;
        }

        @Override
        public Optional<String> post(HttpUrl url, RequestBody body) {
            throw new UnsupportedOperationException("Only pages that are read with GET are recorded");
        }

        // Sorted by URL, so a new recording of the same pages differs only where the pages do
        void save(Path file) throws IOException {
            try (var gzip = new GZIPOutputStream(Files.newOutputStream(file))) {
                JSON.writeValue(gzip, new TreeMap<>(pagesByUrl));
            }
        }
    }
}
