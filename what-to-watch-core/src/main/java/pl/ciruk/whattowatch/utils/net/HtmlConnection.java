package pl.ciruk.whattowatch.utils.net;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import okhttp3.*;
import okhttp3.internal.http.RetryAndFollowUpInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.metrics.Names;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

public class HtmlConnection implements HttpConnection<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OkHttpClient okHttpClient;

    private final Timer requestsTimer;

    public HtmlConnection(OkHttpClient httpClientSupplier) {
        this.okHttpClient = httpClientSupplier;

        this.requestsTimer = Metrics.timer(Names.createName(HtmlConnection.class, "request"));
    }

    @Override
    public Optional<String> connectToAndGet(String url) {
        LOGGER.trace("connectToAndGet - Url: {}", url);

        var requestBuilder = buildRequestTo(url);
        return connectToAndGet(requestBuilder, url);
    }

    @Override
    public Optional<String> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        LOGGER.trace("connectToAndConsume - Url: {}", url);
        var builder = buildRequestTo(url);

        action.accept(builder);
        return connectToAndGet(builder, url);
    }

    private Optional<String> connectToAndGet(Request.Builder requestBuilder, String url) {
        try (var response = execute(requestBuilder)) {
            return Optional.of(response)
                    .filter(Response::isSuccessful)
                    .map(Response::body)
                    .flatMap(responseBody -> extractBodyAsString(responseBody, url));
        } catch (IOException e) {
            LOGGER.warn("connectToAndGet - Could not get {}", url, e);
            return Optional.empty();
        }
    }

    private Optional<String> extractBodyAsString(ResponseBody responseBody, String url) {
        try {
            return Optional.of(responseBody.string());
        } catch (IOException e) {
            LOGGER.warn("connectToAndGet - Could not get {}", url, e);
            return Optional.empty();
        }
    }

    private Response execute(Request.Builder requestBuilder) throws IOException {
        var request = requestBuilder.build();
        try {
            return executeRequest(request);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private Response executeRequest(Request request) throws Exception {
        Call networkCall = okHttpClient.newCall(request);
        try {
            return requestsTimer.recordCallable(networkCall::execute);
        } catch (SocketTimeoutException e) {
            return requestsTimer.recordCallable(networkCall::execute);
        }
    }

    private Request.Builder buildRequestTo(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent", UserAgents.next())
                .addHeader("Accept-Language", "en-US")
                .addHeader("Referer", rootDomainFor(url));
    }

    private static String rootDomainFor(String url) {
        var uri = URI.create(url);
        var port = uri.getPort() > -1
                ? ":" + uri.getPort()
                : "";

        return String.format("%s://%s%s/", uri.getScheme(), uri.getHost(), port);
    }
}
