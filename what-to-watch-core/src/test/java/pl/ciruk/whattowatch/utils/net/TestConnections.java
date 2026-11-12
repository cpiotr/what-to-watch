package pl.ciruk.whattowatch.utils.net;

import okhttp3.OkHttpClient;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;


@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestConnections {
    private TestConnections() {
        throw new AssertionError();
    }

    public static HttpConnection<String> okHttp() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .cookieJar(new InMemoryCookieJar())
                .build();
        return new OkHttpConnection(httpClient, new DomainRateLimiter(domain -> 2));
    }

    public static HttpConnection<String> cached() {
        return new CachedConnection(
                CacheProvider.empty(),
                okHttp());
    }

    public static JsoupConnection jsoup() {
        return new JsoupConnection(okHttp());
    }
}
