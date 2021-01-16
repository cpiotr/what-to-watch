package pl.ciruk.whattowatch.utils.net;


import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Optional;
import java.util.function.Consumer;

public interface HttpConnection<T> {
    Optional<T> connectToAndGet(HttpUrl url);

    Optional<T> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action);

    Optional<T> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action, Consumer<Response> responseConsumer);
}
