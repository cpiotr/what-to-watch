package pl.ciruk.whattowatch.utils.net;

import okhttp3.OkHttpClient;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.List;

@SuppressWarnings({"PMD.ClassNamingConventions", "PMD.TestClassWithoutTestCases"})
public final class TestConnections {
    private TestConnections() {
        throw new AssertionError();
    }

    public static HttpConnection<String> html() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .cookieJar(new InMemoryCookieJar())
                .addInterceptor(new BackoffInterceptor())
                .build();
        return new HtmlConnection(
                httpClient,
                List.of(),
                List.of()
        );
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
