package pl.ciruk.whattowatch.utils.net;


import okhttp3.HttpUrl;
import okhttp3.Request;

import java.util.Optional;
import java.util.function.Consumer;

public interface HttpConnection<T> {
    Optional<T> connectToAndGet(String url);

    default Optional<T> connectToAndGet(HttpUrl url) {
        return connectToAndGet(url.toString());
    }

    Optional<T> connectToAndConsume(String url, Consumer<Request.Builder> action);
}
