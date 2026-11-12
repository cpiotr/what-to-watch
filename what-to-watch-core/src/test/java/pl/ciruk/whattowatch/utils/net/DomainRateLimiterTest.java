package pl.ciruk.whattowatch.utils.net;

import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class DomainRateLimiterTest {
    private static final long INTERVAL = Duration.ofMillis(500).toNanos();
    private static final long SECOND = Duration.ofSeconds(1).toNanos();
    private static final Duration OTHER_DOMAIN_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void shouldSpaceTurnsInArrivalOrder() {
        var schedule = new DomainRateLimiter.Schedule(INTERVAL, 0);

        assertThat(schedule.reserveTurn(0)).isZero();
        assertThat(schedule.reserveTurn(0)).isEqualTo(INTERVAL);
        assertThat(schedule.reserveTurn(100)).isEqualTo(2 * INTERVAL);
    }

    @Test
    void shouldNotSaveUpTurnsWhileIdle() {
        var schedule = new DomainRateLimiter.Schedule(INTERVAL, 0);
        schedule.reserveTurn(0);

        assertThat(schedule.reserveTurn(10 * SECOND)).isEqualTo(10 * SECOND);
        assertThat(schedule.reserveTurn(10 * SECOND)).isEqualTo(10 * SECOND + INTERVAL);
    }

    @Test
    void shouldMoveTurnsPastPause() {
        var schedule = new DomainRateLimiter.Schedule(INTERVAL, 0);

        var pause = schedule.pause(0, Optional.of(Duration.ofSeconds(3)));

        assertThat(pause).contains(Duration.ofSeconds(3));
        assertThat(schedule.isPausedAt(SECOND)).isTrue();
        assertThat(schedule.reserveTurn(SECOND)).isEqualTo(3 * SECOND);
        assertThat(schedule.isPausedAt(3 * SECOND)).isFalse();
    }

    @Test
    void shouldDoublePauseUntilDomainAnswers() {
        var schedule = new DomainRateLimiter.Schedule(INTERVAL, 0);

        assertThat(schedule.pause(0, Optional.empty())).contains(Duration.ofSeconds(1));
        assertThat(schedule.pause(SECOND, Optional.empty())).contains(Duration.ofSeconds(2));
        assertThat(schedule.pause(3 * SECOND, Optional.empty())).contains(Duration.ofSeconds(4));

        schedule.endPauses();

        assertThat(schedule.pause(7 * SECOND, Optional.empty())).contains(Duration.ofSeconds(1));
    }

    @Test
    void shouldCapPause() {
        var schedule = new DomainRateLimiter.Schedule(INTERVAL, 0);

        assertThat(schedule.pause(0, Optional.of(Duration.ofHours(1)))).contains(Duration.ofSeconds(64));
        for (int pause = 1; pause < 10; pause++) {
            schedule.pause(pause * 100 * SECOND, Optional.empty());
        }
        assertThat(schedule.pause(1_000 * SECOND, Optional.empty())).contains(Duration.ofSeconds(64));
    }

    @Test
    void shouldNotExtendPauseWithAnswersToEarlierRequests() {
        var schedule = new DomainRateLimiter.Schedule(INTERVAL, 0);
        schedule.pause(0, Optional.of(Duration.ofSeconds(2)));

        var secondPause = schedule.pause(SECOND, Optional.of(Duration.ofSeconds(2)));

        assertThat(secondPause).isEmpty();
        assertThat(schedule.reserveTurn(SECOND)).isEqualTo(2 * SECOND);
    }

    @Test
    void shouldWaitForTurn() throws InterruptedException {
        var limiter = new DomainRateLimiter(domain -> 20);
        var url = HttpUrl.get("https://www.filmweb.pl/film");

        long start = System.nanoTime();
        for (int request = 0; request < 3; request++) {
            limiter.awaitTurn(url);
        }

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(100));
    }

    @Test
    void shouldPauseDomainForRetryAfter() throws InterruptedException {
        var limiter = new DomainRateLimiter(domain -> 1_000);
        var url = HttpUrl.get("https://www.metacritic.com/movie/x");

        limiter.recordResponse(url, response(url, 429, "1"));

        long start = System.nanoTime();
        limiter.awaitTurn(url);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(900));
    }

    @Test
    void shouldMakeNextRequestWaitForRedirect() throws InterruptedException {
        var limiter = new DomainRateLimiter(domain -> 1);
        var url = HttpUrl.get("https://ekino.ws/movie");

        limiter.recordRedirect(url);

        long start = System.nanoTime();
        limiter.awaitTurn(url);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(900));
    }

    // A wrongly shared pause would wait a minute, far past the timeout
    @Test
    void shouldNotPauseOtherDomains() {
        var limiter = new DomainRateLimiter(domain -> 1_000);
        var url = HttpUrl.get("https://www.metacritic.com/movie/x");

        limiter.recordResponse(url, response(url, 429, "60"));

        assertTimeoutPreemptively(OTHER_DOMAIN_TIMEOUT, () -> limiter.awaitTurn(HttpUrl.get("https://www.filmweb.pl/film")));
    }

    @Test
    void shouldKeepHostsWithoutPrivateDomainApart() {
        var limiter = new DomainRateLimiter(domain -> 1_000);
        var localhost = HttpUrl.get("http://localhost:8080/");

        limiter.recordResponse(localhost, response(localhost, 429, "60"));

        assertTimeoutPreemptively(OTHER_DOMAIN_TIMEOUT, () -> limiter.awaitTurn(HttpUrl.get("http://127.0.0.1:8080/")));
    }

    private static Response response(HttpUrl url, int code, String retryAfter) {
        return new Response.Builder()
                .request(new Request.Builder().url(url).build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .header("Retry-After", retryAfter)
                .build();
    }
}
