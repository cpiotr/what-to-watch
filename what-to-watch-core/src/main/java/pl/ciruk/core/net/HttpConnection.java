package pl.ciruk.core.net;


import okhttp3.Request;

import java.util.Optional;
import java.util.function.Consumer;

public interface HttpConnection<T> {
    Optional<T> connectToAndGet(String url);

    Optional<T> connectToAndConsume(String url, Consumer<Request.Builder> action);
}
