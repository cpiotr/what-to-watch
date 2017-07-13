package pl.ciruk.core.net;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;

@Slf4j
public class HtmlConnection implements HttpConnection<String> {
    private final Supplier<OkHttpClient> httpClientSupplier;

    private final Timer requests;

    public HtmlConnection(Supplier<OkHttpClient> httpClientSupplier, MetricRegistry metricRegistry) {
        this.httpClientSupplier = httpClientSupplier;

        this.requests = metricRegistry.timer(name(HtmlConnection.class, "requests"));
    }

    @PostConstruct
    public void init() {
        log.debug("init: HttpClient: {}", httpClientSupplier);
    }

    @Override
    public Optional<String> connectToAndGet(String url) {
        log.trace("connectToAndGet- Url: {}", url);

        try {
            Response response = execute(to(url));
                return Optional.ofNullable(response)
                    .filter(Response::isSuccessful)
                    .map(Response::body)
                    .flatMap(responseBody -> extractBodyAsString(responseBody, url));
        } catch (IOException e) {
            log.warn("connectToAndGet - Could no get {}", url, e);
            return Optional.empty();
        }
    }

    private Optional<String> extractBodyAsString(ResponseBody responseBody, String url) {
        try {
            return Optional.of(responseBody.string());
        } catch (IOException e) {
            log.warn("connectToAndGet - Could no get {}", url, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        log.trace("connectToAndConsume - Url: {}", url);
        Request.Builder builder = to(url);

        action.accept(builder);
        try {
            Response response = execute(builder);
            return Optional.ofNullable(response.body().string());
        } catch (IOException e) {
            log.warn("Cannot process request to {}", url, e);
            return Optional.empty();
        }
    }

    private Response execute(Request.Builder requestBuilder) throws IOException {
        Request build = requestBuilder.build();
        OkHttpClient okHttpClient = httpClientSupplier.get();
        setTimeouts(okHttpClient);

        Timer.Context time = requests.time();
        try {
            return okHttpClient.newCall(build).execute();
        } finally {
            time.stop();
        }
    }

    private Request.Builder to(String url) {
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

    private void setTimeouts(OkHttpClient httpClient) {
        httpClient.setConnectTimeout(10, TimeUnit.SECONDS);
        httpClient.setReadTimeout(10, TimeUnit.SECONDS);
    }
}
