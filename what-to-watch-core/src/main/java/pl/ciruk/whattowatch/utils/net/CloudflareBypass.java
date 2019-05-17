package pl.ciruk.whattowatch.utils.net;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.script.ScriptEngine;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class CloudflareBypass implements ResponseProcessor {
    private final OkHttpClient httpClient;
    private final JavascriptChallengeSolver challengeSolver;

    public CloudflareBypass(OkHttpClient httpClient, ScriptEngine engine) {
        this.httpClient = httpClient;
        challengeSolver = new JavascriptChallengeSolver(engine);
    }

    @Override
    public Response apply(Response response) {
        if (response.header("CF-RAY") == null) {
            return response;
        }

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(6));

        Request request = response.request();
        try {
            HttpUrl solvedUrl = challengeSolver.solve(request.url(), response.body().string());
            var requestBuilder = request.newBuilder()
                    .url(solvedUrl)
                    .removeHeader(Headers.REFERER)
                    .addHeader(Headers.REFERER, request.url().toString());
            return httpClient.newCall(requestBuilder.build()).execute();
        } catch (IOException e) {
            throw new ResponseProcessingException(e);
        }
    }
}
