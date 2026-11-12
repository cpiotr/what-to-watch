package pl.ciruk.whattowatch.utils.net;


import okhttp3.HttpUrl;
import okhttp3.RequestBody;

import java.util.Optional;

/**
 * Empty when the page doesn't exist. A failed request, such as a timeout or a throttled answer, throws a
 * {@link RequestException} instead, so it isn't mistaken for a missing page.
 */
public interface HttpConnection<T> {
    Optional<T> get(HttpUrl url);

    Optional<T> post(HttpUrl url, RequestBody body);
}
