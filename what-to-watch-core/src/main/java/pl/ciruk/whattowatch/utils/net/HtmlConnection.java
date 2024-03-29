package pl.ciruk.whattowatch.utils.net;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.concurrent.Threads;
import pl.ciruk.whattowatch.utils.metrics.Names;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static pl.ciruk.whattowatch.utils.stream.Functions.consumeNothing;
import static pl.ciruk.whattowatch.utils.stream.Functions.identity;

public class HtmlConnection implements HttpConnection<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OkHttpClient okHttpClient;
    private final List<RequestProcessor> requestProcessors;
    private final List<ResponseProcessor> responseProcessors;
    private final Timer requestsTimer;

    public HtmlConnection(OkHttpClient httpClient) {
        this(httpClient, List.of(), List.of());
    }

    public HtmlConnection(
            OkHttpClient httpClient,
            List<RequestProcessor> requestProcessors,
            List<ResponseProcessor> responseProcessors) {
        this.okHttpClient = httpClient;
        this.requestProcessors = requestProcessors;
        this.responseProcessors = responseProcessors;

        this.requestsTimer = Metrics.timer(Names.createName(HtmlConnection.class, "request"));
    }

    @Override
    public Optional<String> connectToAndGet(HttpUrl url) {
        LOGGER.trace("Url: {}", url);

        var requestBuilder = buildRequestTo(url);
        return connectToAndGet(requestBuilder, url, consumeNothing());
    }

    @Override
    public Optional<String> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action) {
        LOGGER.trace("Url: {}", url);
        var builder = buildRequestTo(url);

        action.accept(builder);
        return connectToAndGet(builder, url, consumeNothing());
    }

    @Override
    public Optional<String> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action, Consumer<Response> responseConsumer) {
        LOGGER.trace("Url: {}", url);
        var builder = buildRequestTo(url);

        action.accept(builder);
        return connectToAndGet(builder, url, responseConsumer);
    }

    private Optional<String> connectToAndGet(Request.Builder requestBuilder, HttpUrl url, Consumer<Response> responseConsumer) {
        try (var response = execute(requestBuilder)) {
            responseConsumer.accept(response);
            return Optional.of(response)
                    .flatMap(this::unwrapBody)
                    .map(identity(this::logBodyIfUnsuccessful))
                    .filter(ProcessedResponse::successful)
                    .map(ProcessedResponse::body);
        } catch (RetryableException e) {
            LOGGER.warn("Could not get {} due to: {}", url, e.getMessage());
        } catch (HtmlConnectionException e) {
            LOGGER.warn("Could not get {}", url, e);
        }
        return Optional.empty();
    }

    @SuppressWarnings("PMD.AvoidRethrowingException")
    private Response execute(Request.Builder requestBuilder) {
        var request = requestBuilder.build();
        try {
            return requestsTimer.recordCallable(() -> executeRequest(request));
        } catch (HtmlConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new HtmlConnectionException(e);
        }
    }

    private void logBodyIfUnsuccessful(ProcessedResponse processedResponse) {
        if (!processedResponse.successful()) {
            LOGGER.trace("Erroneous response: {}", processedResponse.body());
        }
    }

    private Optional<ProcessedResponse> unwrapBody(Response response) {
        return extractBodyAsString(response.body(), response.request().url())
                .map(body -> new ProcessedResponse(body, response.isSuccessful()));
    }

    private Optional<String> extractBodyAsString(ResponseBody responseBody, HttpUrl url) {
        try {
            return Optional.of(responseBody.string());
        } catch (SocketTimeoutException e) {
            LOGGER.warn("Timeout while getting {}", url);
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.warn("Could not get {}", url, e);
            return Optional.empty();
        }
    }

    private Response executeRequest(Request request) {
        Request processedRequest = processRequest(request);
        try {
            return executeOrWait(processedRequest);
        } catch (RetryableException e) {
            return executeOrWait(processedRequest);
        }
    }

    private Response executeOrWait(Request request) {
        return Threads.manageBlocking(() -> {
            try {
                Response response = okHttpClient.newCall(request).execute();
                LOGGER.trace("Got response {} from: {}", response.code(), request.url());
                response = processResponse(response);
                return response;
            } catch (SocketTimeoutException e) {
                throw new RetryableException(e);
            } catch (Exception e) {
                throw new NonRetryableException(e);
            }
        });
    }

    private Request processRequest(Request request) {
        var processedRequest = request.newBuilder().build();
        for (var requestProcessor : requestProcessors) {
            processedRequest = requestProcessor.process(processedRequest);
        }
        return processedRequest;
    }

    private Response processResponse(Response response) {
        Response processedResponse = response.newBuilder().build();
        for (var responseProcessor : responseProcessors) {
            var nextResponse = responseProcessor.process(processedResponse);
            processedResponse.close();
            processedResponse = nextResponse;
            LOGGER.trace("Processed response code: {}", processedResponse.code());
        }
        return processedResponse;
    }

    private Request.Builder buildRequestTo(HttpUrl url) {
        HttpUrl referer = Optional.ofNullable(url.resolve("/"))
                .orElse(url);
        return new Request.Builder()
                .url(url)
                .addHeader(Headers.USER_AGENT, UserAgents.next())
                .addHeader("Accept-Language", "en-US")
                .addHeader(Headers.REFERER, referer.toString())
                .addHeader("Cache-Control", "private");
    }

    static class HtmlConnectionException extends RuntimeException {
        HtmlConnectionException(Throwable cause) {
            super(cause);
        }
    }

    static class RetryableException extends HtmlConnectionException {
        RetryableException(Throwable cause) {
            super(cause);
        }
    }

    static class NonRetryableException extends HtmlConnectionException {
        NonRetryableException(Throwable cause) {
            super(cause);
        }
    }

}
