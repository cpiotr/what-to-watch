package pl.ciruk.whattowatch.utils.net;


import okhttp3.HttpUrl;
import okhttp3.RequestBody;

import java.util.Optional;

public interface HttpConnection<T> {
    Optional<T> get(HttpUrl url);

    Optional<T> post(HttpUrl url, RequestBody body);
}
