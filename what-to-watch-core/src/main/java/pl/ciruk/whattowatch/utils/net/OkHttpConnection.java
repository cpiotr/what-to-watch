package pl.ciruk.whattowatch.utils.net;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.concurrent.Threads;
import pl.ciruk.whattowatch.utils.metrics.Names;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.SocketTimeoutException;
import java.util.Optional;

public class OkHttpConnection implements HttpConnection<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int NOT_FOUND = 404;
    private static final int GONE = 410;

    private final OkHttpClient okHttpClient;
    private final DomainRateLimiter rateLimiter;
    private final Timer requestsTimer;

    public OkHttpConnection(OkHttpClient okHttpClient, DomainRateLimiter rateLimiter) {
        this.okHttpClient = okHttpClient;
        this.rateLimiter = rateLimiter;

        this.requestsTimer = Metrics.timer(Names.createName(OkHttpConnection.class, "request"));
    }

    @Override
    public Optional<String> get(HttpUrl url) {
        LOGGER.trace("Url: {}", url);
        return bodyOf(buildRequestTo(url).build());
    }

    @Override
    public Optional<String> post(HttpUrl url, RequestBody body) {
        LOGGER.trace("Url: {}", url);
        return bodyOf(buildRequestTo(url).post(body).build());
    }

    private Optional<String> bodyOf(Request request) {
        var url = request.url();
        try (var response = executeRequest(request); var responseBody = response.body()) {
            if (response.code() == NOT_FOUND || response.code() == GONE) {
                LOGGER.debug("No page at {}", url);
                return Optional.empty();
            }
            var body = responseBody == null ? "" : responseBody.string();
            // An empty page, such as IMDb's bot challenge (202 with no body), is a failure too
            if (!response.isSuccessful() || body.isEmpty()) {
                LOGGER.trace("Erroneous response {} from {}: {}", response.code(), url, body);
                throw new RequestException("Got " + response.code() + " from " + url);
            }
            return Optional.of(body);
        } catch (IOException e) {
            throw new RequestException("Could not read " + url, e);
        }
    }

    private Response executeRequest(Request request) {
        try {
            return executeOnce(request);
        } catch (RetryableException e) {
            return executeOnce(request);
        }
    }

    private Response executeOnce(Request request) {
        awaitTurn(request.url());
        return Threads.manageBlocking(() -> {
            var sample = Timer.start();
            try {
                Response response = okHttpClient.newCall(request).execute();
                LOGGER.trace("Got response {} from: {}", response.code(), response.request().url());
                recordRedirects(response);
                rateLimiter.recordResponse(response.request().url(), response);
                return response;
            } catch (SocketTimeoutException e) {
                rateLimiter.recordFailure(request.url());
                throw new RetryableException("Timed out getting " + request.url(), e);
            } catch (IOException e) {
                rateLimiter.recordFailure(request.url());
                throw new RequestException("Could not get " + request.url(), e);
            } catch (RuntimeException e) {
                throw new RequestException("Could not get " + request.url(), e);
            } finally {
                sample.stop(requestsTimer);
            }
        });
    }

    // Waits outside managedBlock: a thread blocked inside it makes a ForkJoinPool start another one
    private void awaitTurn(HttpUrl url) {
        try {
            rateLimiter.awaitTurn(url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestException("Interrupted while waiting to get " + url, e);
        }
    }

    // OkHttp follows redirects without waiting for a turn, so each one takes a turn afterwards.
    // OkHttp has already closed the responses that redirected.
    @SuppressWarnings("PMD.CloseResource")
    private void recordRedirects(Response response) {
        var redirect = response.priorResponse();
        if (redirect != null) {
            rateLimiter.recordRedirect(response.request().url());
            recordRedirects(redirect);
        }
    }

    private static Request.Builder buildRequestTo(HttpUrl url) {
        HttpUrl referer = Optional.ofNullable(url.resolve("/"))
                .orElse(url);
        return new Request.Builder()
                .url(url)
                .addHeader("Accept-Language", "en-US")
                .addHeader("Referer", referer.toString())
                .addHeader("Cache-Control", "private");
    }

    private static final class RetryableException extends RequestException {
        RetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
