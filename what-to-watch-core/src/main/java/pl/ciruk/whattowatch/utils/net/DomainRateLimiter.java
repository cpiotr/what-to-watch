package pl.ciruk.whattowatch.utils.net;

import okhttp3.HttpUrl;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.ToDoubleFunction;

/**
 * Spaces requests to each domain evenly, in the order they arrive, and pauses a domain that asks for less traffic
 * (429 or 503) or fails to answer.
 */
public class DomainRateLimiter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Duration FIRST_PAUSE = Duration.ofSeconds(1);
    private static final Duration LONGEST_PAUSE = Duration.ofSeconds(64);
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int SERVICE_UNAVAILABLE = 503;

    private final ToDoubleFunction<String> requestsPerSecondByDomain;
    private final Map<String, Schedule> schedules = new ConcurrentHashMap<>();

    public DomainRateLimiter(ToDoubleFunction<String> requestsPerSecondByDomain) {
        this.requestsPerSecondByDomain = requestsPerSecondByDomain;
    }

    public void awaitTurn(HttpUrl url) throws InterruptedException {
        var schedule = scheduleFor(url);
        sleepUntil(schedule.reserveTurn(System.nanoTime()));
        // A pause that started while this request waited applies to it too
        while (schedule.isPausedAt(System.nanoTime())) {
            sleepUntil(schedule.reserveTurn(System.nanoTime()));
        }
    }

    public void recordResponse(HttpUrl url, Response response) {
        int code = response.code();
        if (code == TOO_MANY_REQUESTS || code == SERVICE_UNAVAILABLE) {
            pause(url, retryAfter(response));
        } else {
            scheduleFor(url).endPauses();
        }
    }

    public void recordFailure(HttpUrl url) {
        pause(url, Optional.empty());
    }

    // A redirect was sent without waiting, so the domain's next request waits for it instead
    public void recordRedirect(HttpUrl url) {
        scheduleFor(url).reserveTurn(System.nanoTime());
    }

    private void pause(HttpUrl url, Optional<Duration> requested) {
        scheduleFor(url).pause(System.nanoTime(), requested)
                .ifPresent(duration -> LOGGER.info("Pausing requests to {} for {}", domainOf(url), duration));
    }

    private Schedule scheduleFor(HttpUrl url) {
        return schedules.computeIfAbsent(domainOf(url), this::createSchedule);
    }

    private Schedule createSchedule(String domain) {
        double requestsPerSecond = requestsPerSecondByDomain.applyAsDouble(domain);
        LOGGER.info("Limiting requests to {} to {} per second", domain, requestsPerSecond);
        long interval = Math.round(Duration.ofSeconds(1).toNanos() / requestsPerSecond);
        return new Schedule(interval, System.nanoTime());
    }

    // IP addresses and localhost have no top private domain
    private static String domainOf(HttpUrl url) {
        return Objects.requireNonNullElse(url.topPrivateDomain(), url.host());
    }

    // An HTTP date isn't supported and gets the default pause instead
    private static Optional<Duration> retryAfter(Response response) {
        try {
            return Optional.ofNullable(response.header("Retry-After"))
                    .map(value -> Long.parseLong(value.strip()))
                    .filter(seconds -> seconds >= 0)
                    .map(Duration::ofSeconds);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static void sleepUntil(long turn) throws InterruptedException {
        long wait = turn - System.nanoTime();
        if (wait > 0) {
            Thread.sleep(Duration.ofNanos(wait));
        }
    }

    // Times are System.nanoTime() values. A lock rather than synchronized, which pins a virtual thread on JDK 21.
    static final class Schedule {
        private final ReentrantLock lock = new ReentrantLock();
        private final long interval;
        private long nextTurn;
        private long pausedUntil;
        private int consecutivePauses;

        Schedule(long interval, long now) {
            this.interval = interval;
            this.nextTurn = now;
            this.pausedUntil = now;
        }

        long reserveTurn(long now) {
            lock.lock();
            try {
                long turn = Math.max(now, Math.max(nextTurn, pausedUntil));
                nextTurn = turn + interval;
                return turn;
            } finally {
                lock.unlock();
            }
        }

        boolean isPausedAt(long now) {
            lock.lock();
            try {
                return pausedUntil - now > 0;
            } finally {
                lock.unlock();
            }
        }

        // Throttled answers to requests sent before the pause don't extend it
        Optional<Duration> pause(long now, Optional<Duration> requested) {
            lock.lock();
            try {
                if (pausedUntil - now > 0) {
                    return Optional.empty();
                }
                consecutivePauses++;
                var duration = requested.orElseGet(this::doublingPause);
                if (duration.compareTo(LONGEST_PAUSE) > 0) {
                    duration = LONGEST_PAUSE;
                }
                pausedUntil = now + duration.toNanos();
                return Optional.of(duration);
            } finally {
                lock.unlock();
            }
        }

        void endPauses() {
            lock.lock();
            try {
                consecutivePauses = 0;
            } finally {
                lock.unlock();
            }
        }

        private Duration doublingPause() {
            return FIRST_PAUSE.multipliedBy(1L << Math.min(consecutivePauses - 1, 6));
        }
    }
}
