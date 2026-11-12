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
        try (var response = executeRequest(request); var responseBody = response.body()) {
            var body = responseBody == null ? "" : responseBody.string();
            // An empty page, such as IMDb's bot challenge (202 with no body), is useless and mustn't be cached
            if (!response.isSuccessful() || body.isEmpty()) {
                LOGGER.trace("Erroneous response {} from {}: {}", response.code(), request.url(), body);
                return Optional.empty();
            }
            return Optional.of(body);
        } catch (RetryableException | SocketTimeoutException e) {
            LOGGER.warn("Could not get {} due to: {}", request.url(), e.getMessage());
        } catch (RequestException | IOException e) {
            LOGGER.warn("Could not get {}", request.url(), e);
        }
        return Optional.empty();
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
                throw new RetryableException(e);
            } catch (IOException e) {
                rateLimiter.recordFailure(request.url());
                throw new RequestException(e);
            } catch (RuntimeException e) {
                throw new RequestException(e);
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
            throw new RequestException(e);
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

    static class RequestException extends RuntimeException {
        RequestException(Throwable cause) {
            super(cause);
        }
    }

    static class RetryableException extends RequestException {
        RetryableException(Throwable cause) {
            super(cause);
        }
    }
}
