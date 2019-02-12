package pl.ciruk.whattowatch.utils.net;


import okhttp3.HttpUrl;
import okhttp3.Request;

import java.util.Optional;
import java.util.function.Consumer;

public interface HttpConnection<T> {
    default Optional<T> connectToAndGet(String url) {
        return connectToAndGet(HttpUrl.get(url));
    }

    Optional<T> connectToAndGet(HttpUrl url);

    Optional<T> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action);

    default Optional<T> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        return connectToAndConsume(HttpUrl.get(url), action);
    }
}
