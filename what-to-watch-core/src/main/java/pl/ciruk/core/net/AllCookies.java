package pl.ciruk.core.net;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import lombok.extern.slf4j.Slf4j;

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
        client.setCookieHandler(cookieManager);
        client.networkInterceptors().add(this::handleCookies);
        client.setFollowRedirects(true);
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
