package pl.ciruk.whattowatch.utils.net;

import okhttp3.OkHttpClient;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class TestConnections {
    private TestConnections() {
        throw new AssertionError();
    }

    public static HtmlConnection html() {
        return new HtmlConnection(new OkHttpClient());
    }

    public static JsoupConnection jsoup() {
        return new JsoupConnection(html());
    }
}