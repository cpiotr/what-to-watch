package pl.ciruk.whattowatch.utils.net;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.metrics.Names;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class HtmlConnection implements HttpConnection<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int BACKOFF_THRESHOLD = 2;

    private final OkHttpClient okHttpClient;
    private final Timer requestsTimer;
    private final Map<String, AtomicInteger> errorsByDomain = new ConcurrentHashMap<>();
    private final IntConsumer backOffFunction;

    public HtmlConnection(OkHttpClient httpClient) {
        this(httpClient, HtmlConnection::backOff);
    }

    HtmlConnection(OkHttpClient httpClient, IntConsumer backOffFunction) {
        this.okHttpClient = httpClient;
        this.backOffFunction = backOffFunction;

        this.requestsTimer = Metrics.timer(Names.createName(HtmlConnection.class, "request"));
    }

    @Override
    public Optional<String> connectToAndGet(HttpUrl url) {
        LOGGER.trace("Url: {}", url);

        var requestBuilder = buildRequestTo(url);
        return connectToAndGet(requestBuilder, url);
    }

    @Override
    public Optional<String> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action) {
        LOGGER.trace("Url: {}", url);
        var builder = buildRequestTo(url);

        action.accept(builder);
        return connectToAndGet(builder, url);
    }

    private Optional<String> connectToAndGet(Request.Builder requestBuilder, HttpUrl url) {
        try (var response = execute(requestBuilder)) {
            return Optional.of(response)
                    .filter(Response::isSuccessful)
                    .map(Response::body)
                    .flatMap(responseBody -> extractBodyAsString(responseBody, url));
        } catch (IOException e) {
            LOGGER.warn("Could not get {}", url, e);
            return Optional.empty();
        }
    }

    private Optional<String> extractBodyAsString(ResponseBody responseBody, HttpUrl url) {
        try {
            return Optional.of(responseBody.string());
        } catch (IOException e) {
            LOGGER.warn("Could not get {}", url, e);
            return Optional.empty();
        }
    }

    private Response execute(Request.Builder requestBuilder) throws IOException {
        var request = requestBuilder.build();
        try {
            return requestsTimer.recordCallable(() -> executeRequest(request));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private Response executeRequest(Request request) throws IOException {
        try {
            return executeOrWait(request);
        } catch (SocketTimeoutException e) {
            return executeOrWait(request);
        }
    }

    private Response executeOrWait(Request request) throws IOException {
        String domain = request.url().topPrivateDomain();
        AtomicInteger errors = errorsByDomain.computeIfAbsent(domain, key -> new AtomicInteger());
        int errorsValue = errors.get();
        if (errorsValue >= BACKOFF_THRESHOLD) {
            LOGGER.info("Backing off of {}", domain);
            backOffFunction.accept(errorsValue);
        }

        try {
            Response response = okHttpClient.newCall(request).execute();
            if (errorsValue > 0) {
                errors.decrementAndGet();
            }
            return response;
        } catch (IOException e) {
            errors.incrementAndGet();
            throw e;
        }
    }

    private Request.Builder buildRequestTo(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent", UserAgents.next())
                .addHeader("Accept-Language", "en-US")
                .addHeader("Referer", url.resolve("/").toString());
    }

    private static void backOff(int errorsValue) {
        double exponential = Math.pow(2, errorsValue % 6);
        long waitTimeMillis = (long) (exponential * 100);
        LOGGER.debug("Waiting {} ms", waitTimeMillis);
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitTimeMillis));
    }
}
