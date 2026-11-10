package pl.ciruk.whattowatch.utils.net;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCookieJarTest {
    private static final HttpUrl URL = HttpUrl.get("https://www.example.com/");
    private static final String SESSION = "session";

    private final InMemoryCookieJar cookieJar = new InMemoryCookieJar();

    @Test
    void shouldReplaceCookieWithSameName() {
        cookieJar.saveFromResponse(URL, List.of(cookie(SESSION, "old")));

        cookieJar.saveFromResponse(URL, List.of(cookie(SESSION, "new")));

        assertThat(cookieJar.loadForRequest(URL))
                .extracting(Cookie::value)
                .containsExactly("new");
    }

    @Test
    void shouldDeleteCookieWhenServerExpiresIt() {
        cookieJar.saveFromResponse(URL, List.of(cookie(SESSION, "value"), cookie("other", "value")));

        cookieJar.saveFromResponse(URL, List.of(expiredCookie(SESSION)));

        assertThat(cookieJar.loadForRequest(URL))
                .extracting(Cookie::name)
                .containsExactly("other");
    }

    @Test
    void shouldKeepAllCookiesSavedConcurrently() {
        try (var pool = Executors.newFixedThreadPool(8)) {
            var saves = IntStream.range(0, 1_000)
                    .mapToObj(i -> CompletableFuture.runAsync(
                            () -> cookieJar.saveFromResponse(URL, List.of(cookie("cookie" + i, "value"))),
                            pool))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(saves).join();
        }

        assertThat(cookieJar.loadForRequest(URL)).hasSize(1_000);
    }

    private static Cookie cookie(String name, String value) {
        return new Cookie.Builder()
                .domain(URL.host())
                .name(name)
                .value(value)
                .build();
    }

    private static Cookie expiredCookie(String name) {
        return new Cookie.Builder()
                .domain(URL.host())
                .name(name)
                .value("")
                .expiresAt(1)
                .build();
    }
}
