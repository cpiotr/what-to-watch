package pl.ciruk.whattowatch.utils.net;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class InMemoryCookieJar implements CookieJar {
    private final Map<String, List<Cookie>> cookiesByDomain = new ConcurrentHashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        cookiesByDomain.merge(domainOf(url), cookies, InMemoryCookieJar::replaceWith);
    }

    // Stored by domain, since a cookie can be set for the whole domain rather than one host
    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        var now = System.currentTimeMillis();
        return cookiesByDomain.getOrDefault(domainOf(url), List.of()).stream()
                .filter(cookie -> cookie.expiresAt() > now)
                .filter(cookie -> cookie.matches(url))
                .toList();
    }

    // IP addresses and localhost have no top private domain
    private static String domainOf(HttpUrl url) {
        return Objects.requireNonNullElse(url.topPrivateDomain(), url.host());
    }

    // A received cookie replaces the stored one with the same name, domain and path; an expired one deletes it
    private static List<Cookie> replaceWith(List<Cookie> stored, List<Cookie> received) {
        var now = System.currentTimeMillis();
        return Stream.concat(
                        stored.stream().filter(cookie -> received.stream().noneMatch(update -> isSameCookie(cookie, update))),
                        received.stream())
                .filter(cookie -> cookie.expiresAt() > now)
                .toList();
    }

    private static boolean isSameCookie(Cookie first, Cookie second) {
        return first.name().equals(second.name())
                && first.domain().equals(second.domain())
                && first.path().equals(second.path());
    }
}
