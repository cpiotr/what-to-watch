package pl.ciruk.core.net;

import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.core.cache.CacheProvider;

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
        LOGGER.trace("connectToAndGet - Url: {}", url);

        Optional<String> document = cache.get(url);
        if (!document.isPresent()) {
            LOGGER.trace("connectToAndGet - Cache miss for: {}", url);
            document = connection.connectToAndGet(url);
            document.ifPresent(content -> cache.put(url, content));
        }
        return document;
    }

    @Override
    public Optional<String> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        LOGGER.trace("connectToAndConsume - Url: {}", url);
        LOGGER.trace("connectToAndConsume - Method currently does not rely on cache");

        return connection.connectToAndConsume(url, action);
    }
}
