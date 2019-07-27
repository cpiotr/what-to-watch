package pl.ciruk.whattowatch.utils.net;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.script.ScriptEngine;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

public class CloudflareBypass implements ResponseProcessor {
    private static final int TEMPORARILY_UNAVAILABLE = 503;

    private final OkHttpClient httpClient;
    private final JavascriptChallengeSolver challengeSolver;
    private final Duration timeout;

    public CloudflareBypass(OkHttpClient httpClient, ScriptEngine engine, Duration timeout) {
        this.httpClient = httpClient;
        challengeSolver = new JavascriptChallengeSolver(engine);
        this.timeout = timeout;
    }

    @Override
    public Response apply(Response response) {
        if (qualifiesForChallenge(response)) {
            return response;
        }

        LockSupport.parkNanos(timeout.toNanos());

        Request request = response.request();
        try {
            var solvedUrl = challengeSolver.solve(request.url(), response.body().string());
            var requestBuilder = request.newBuilder()
                    .url(solvedUrl)
                    .removeHeader(Headers.REFERER)
                    .addHeader(Headers.REFERER, request.url().toString());
            return httpClient.newCall(requestBuilder.build()).execute();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean qualifiesForChallenge(Response response) {
        return response.header("CF-RAY") == null || response.code() != TEMPORARILY_UNAVAILABLE;
    }
}
