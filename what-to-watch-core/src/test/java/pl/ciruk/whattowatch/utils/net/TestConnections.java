package pl.ciruk.whattowatch.utils.net;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class TestConnections {
    private TestConnections() {
        throw new AssertionError();
    }

    public static HtmlConnection html() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .cookieJar(new CookieJar() {
                    private final Map<String, List<Cookie>> cookiesByHost = new ConcurrentHashMap<>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        List<Cookie> existingCookies = cookiesByHost.computeIfAbsent(url.host(), __ -> new ArrayList<>());
                        existingCookies.addAll(cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        return cookiesByHost.getOrDefault(url.host(), new ArrayList<>());
                    }
                })
                .build();

        return new HtmlConnection(httpClient);
    }

    public static JsoupConnection jsoup() {
        return new JsoupConnection(html());
    }
}