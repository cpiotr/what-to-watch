package pl.ciruk.core.net;

import com.squareup.okhttp.OkHttpClient;

public interface CookiePolicy {
    void applyTo(OkHttpClient client);
}
