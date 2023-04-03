package pl.ciruk.whattowatch.utils.net;

import okhttp3.OkHttpClient;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.List;

@SuppressWarnings({"PMD.ClassNamingConventions", "PMD.TestClassWithoutTestCases"})
public final class TestConnections {
    private TestConnections() {
        throw new AssertionError();
    }

    public static HtmlConnection html() {
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

    public static JsoupConnection jsoup() {
        return new JsoupConnection(html());
    }
}
