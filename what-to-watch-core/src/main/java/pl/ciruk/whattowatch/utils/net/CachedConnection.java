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
    // A page is never empty, as the connection fails on an empty answer, so an empty value means there's no page
    private static final String NO_PAGE = "";

    private final CacheProvider<String> cache;
    private final HttpConnection<String> connection;

    public CachedConnection(CacheProvider<String> cache, HttpConnection<String> connection) {
        this.cache = cache;
        this.connection = connection;
    }

    @Override
    public Optional<String> get(HttpUrl url) {
        LOGGER.trace("Url: {}", url);

        var cached = cache.get(url.toString());
        if (cached.isPresent()) {
            return cached.filter(content -> !NO_PAGE.equals(content));
        }

        LOGGER.trace("Cache miss for: {}", url);
        // A missing page is cached too, or each search for a title that a site doesn't have waits for a turn again
        var optionalDocument = connection.get(url);
        cache.put(url.toString(), optionalDocument.orElse(NO_PAGE));
        return optionalDocument;
    }

    // The URL alone doesn't identify a POST's answer, so it isn't cached
    @Override
    public Optional<String> post(HttpUrl url, RequestBody body) {
        return connection.post(url, body);
    }
}
