package pl.ciruk.core.net;

import com.squareup.okhttp.Request;
import lombok.extern.slf4j.Slf4j;
import pl.ciruk.core.cache.CacheProvider;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class CachedConnection implements HttpConnection<String> {
    private final CacheProvider<String> cache;

    private final HttpConnection<String> connection;

    public CachedConnection(CacheProvider<String> cache, HttpConnection<String> connection) {
        this.cache = cache;
        this.connection = connection;
    }

    @Override
    public Optional<String> connectToAndGet(String url) {
        log.trace("connectToAndGet - Url: {}", url);

        Optional<String> document = cache.get(url);
        if (!document.isPresent()) {
            log.trace("connectToAndGet - Cache miss for: {}", url);
            document = connection.connectToAndGet(url);
            document.ifPresent(content -> cache.put(url, content));
        }
        return document;
    }

    @Override
    public Optional<String> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        log.trace("connectToAndConsume - Url: {}", url);
        log.trace("connectToAndConsume - Method currently does not rely on cache");

        return connection.connectToAndConsume(url, action);
    }
}
