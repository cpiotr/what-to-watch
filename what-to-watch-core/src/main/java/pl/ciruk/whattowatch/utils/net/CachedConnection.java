package pl.ciruk.whattowatch.utils.net;

import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.function.Consumer;

public class CachedConnection implements HttpConnection<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CacheProvider<String> cache;

    private final HttpConnection<String> connection;

    public CachedConnection(CacheProvider<String> cache, HttpConnection<String> connection) {
        this.cache = cache;
        this.connection = connection;
    }

    @Override
    public Optional<String> connectToAndGet(String url) {
        LOGGER.trace("Url: {}", url);

        var optionalDocument = cache.get(url);
        if (optionalDocument.isEmpty()) {
            LOGGER.trace("Cache miss for: {}", url);
            optionalDocument = connection.connectToAndGet(url);
            optionalDocument.ifPresent(content -> cache.put(url, content));
        }
        return optionalDocument;
    }

    @Override
    public Optional<String> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        LOGGER.trace("Url: {}", url);
        LOGGER.trace("Method currently does not rely on cache");

        return connection.connectToAndConsume(url, action);
    }
}
