package pl.ciruk.core.net;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

import static com.codahale.metrics.MetricRegistry.name;

@Slf4j
public class HtmlConnection implements HttpConnection<String> {
    private final OkHttpClient okHttpClient;

    private final Timer requests;

    public HtmlConnection(OkHttpClient httpClientSupplier, MetricRegistry metricRegistry) {
        this.okHttpClient = httpClientSupplier;

        this.requests = metricRegistry.timer(name(HtmlConnection.class, "requests"));
    }

    @Override
    public Optional<String> connectToAndGet(String url) {
        log.trace("connectToAndGet - Url: {}", url);

        Request.Builder requestBuilder = buildRequestTo(url);
        return connectToAndGet(requestBuilder, url);
    }

    @Override
    public Optional<String> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        log.trace("connectToAndConsume - Url: {}", url);
        Request.Builder builder = buildRequestTo(url);

        action.accept(builder);
        return connectToAndGet(builder, url);
    }

    private Optional<String> connectToAndGet(Request.Builder requestBuilder, String url) {
        try (Response response = execute(requestBuilder)) {
            return Optional.ofNullable(response)
                    .filter(Response::isSuccessful)
                    .map(Response::body)
                    .flatMap(responseBody -> extractBodyAsString(responseBody, url));
        } catch (IOException e) {
            log.warn("connectToAndGet - Could not get {}", url, e);
            return Optional.empty();
        }
    }

    private Optional<String> extractBodyAsString(ResponseBody responseBody, String url) {
        try {
            return Optional.of(responseBody.string());
        } catch (IOException e) {
            log.warn("connectToAndGet - Could not get {}", url, e);
            return Optional.empty();
        }
    }

    private Response execute(Request.Builder requestBuilder) throws IOException {
        Request build = requestBuilder.build();

        Timer.Context time = requests.time();
        try {
            return okHttpClient.newCall(build).execute();
        } finally {
            time.stop();
        }
    }

    private Request.Builder buildRequestTo(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent", UserAgents.next())
                .addHeader("Accept-Language", "en-US")
                .addHeader("Referer", rootDomainFor(url));
    }

    private Response log(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        log.trace("Request: {}", request);

        Response response = chain.proceed(request);
        log.trace("Response: {}", response);

        return response;
    }

    private static String rootDomainFor(String url) {
        URI uri = URI.create(url);
        String port = uri.getPort() > -1
                ? ":" + uri.getPort()
                : "";

        return String.format("%s://%s%s/", uri.getScheme(), uri.getHost(), port);
    }
}
