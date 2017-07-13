package pl.ciruk.core.net;


import okhttp3.OkHttpClient;

public interface CookiePolicy {
    void applyTo(OkHttpClient client);
}
