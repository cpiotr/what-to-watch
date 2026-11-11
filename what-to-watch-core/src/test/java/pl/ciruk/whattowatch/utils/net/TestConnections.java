package pl.ciruk.whattowatch.utils.net;

import okhttp3.OkHttpClient;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;


@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestConnections {
    private TestConnections() {
        throw new AssertionError();
    }

    public static HttpConnection<String> html() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .cookieJar(new InMemoryCookieJar())
                .build();
        return new HtmlConnection(httpClient, new DomainRateLimiter(domain -> 2));
    }

    public static HttpConnection<String> cached() {
        return new CachedConnection(
                CacheProvider.empty(),
                html());
    }

    public static JsoupConnection jsoup() {
        return new JsoupConnection(html());
    }
}
