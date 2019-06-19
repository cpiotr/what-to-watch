package pl.ciruk.whattowatch.utils.net;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCookieJar implements CookieJar {
    private final Map<String, List<Cookie>> cookiesByHost = new ConcurrentHashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        List<Cookie> existingCookies = cookiesByHost.computeIfAbsent(url.host(), ignored -> new ArrayList<>());
        existingCookies.addAll(cookies);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        return cookiesByHost.getOrDefault(url.host(), List.of());
    }
}
