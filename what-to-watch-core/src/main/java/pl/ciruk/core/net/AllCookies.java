package pl.ciruk.core.net;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class AllCookies implements CookiePolicy {

    private Set<String> cookies = new HashSet<>();

    @Override
    public void applyTo(OkHttpClient client) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(java.net.CookiePolicy.ACCEPT_ALL);
        client.networkInterceptors().add(this::handleCookies);
    }

    public Response handleCookies(Interceptor.Chain chain) throws IOException {
        Request.Builder request = chain.request().newBuilder();
        attachCookiesTo(request);

        Response response = chain.proceed(request.build());
        readCookiesFrom(response);
        return response;
    }

    private void readCookiesFrom(Response response) {
        List<String> cookiesFromResponse = response.headers("Set-Cookie");
        log.trace("HTTP interceptor - Received cookies: {}", cookiesFromResponse);
        cookies.addAll(cookiesFromResponse);
    }

    private void attachCookiesTo(Request.Builder request) {
        log.trace("Cookies to be sent: {}", cookies);
        cookies.forEach(cookie -> request.addHeader("Cookie", cookie));
    }
}
