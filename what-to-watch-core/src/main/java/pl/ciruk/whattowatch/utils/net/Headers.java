package pl.ciruk.whattowatch.utils.net;

public final class Headers {
    static final String USER_AGENT = "User-Agent";
    static final String REFERER = "Referer";

    private Headers() {
        throw new AssertionError();
    }
}
