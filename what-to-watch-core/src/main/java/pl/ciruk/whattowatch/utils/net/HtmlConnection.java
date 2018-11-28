package pl.ciruk.whattowatch.utils.net;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
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
        LOGGER.trace("Url: {}", url);

        var requestBuilder = buildRequestTo(url);
        return connectToAndGet(requestBuilder, url);
    }

    @Override
    public Optional<String> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        LOGGER.trace("Url: {}", url);
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
            LOGGER.warn("Could not get {}", url, e);
            return Optional.empty();
        }
    }

    private Optional<String> extractBodyAsString(ResponseBody responseBody, String url) {
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
            return okHttpClient.newCall(request).execute();
        } catch (SocketTimeoutException e) {
            return okHttpClient.newCall(request).execute();
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
