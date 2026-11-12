package pl.ciruk.whattowatch.utils.net;

import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

public class CachedConnection implements HttpConnection<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CacheProvider<String> cache;
    private final HttpConnection<String> connection;

    public CachedConnection(CacheProvider<String> cache, HttpConnection<String> connection) {
        this.cache = cache;
        this.connection = connection;
    }

    @Override
    public Optional<String> get(HttpUrl url) {
        LOGGER.trace("Url: {}", url);

        var optionalDocument = cache.get(url.toString());
        if (optionalDocument.isEmpty()) {
            LOGGER.trace("Cache miss for: {}", url);
            optionalDocument = connection.get(url);
            optionalDocument.ifPresent(content -> addToCache(url, content));
        }
        return optionalDocument;
    }

    private void addToCache(HttpUrl url, String content) {
        cache.put(url.toString(), content);
    }

    // The URL alone doesn't identify a POST's answer, so it isn't cached
    @Override
    public Optional<String> post(HttpUrl url, RequestBody body) {
        return connection.post(url, body);
    }
}
