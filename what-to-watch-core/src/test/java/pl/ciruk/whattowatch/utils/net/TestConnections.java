package pl.ciruk.whattowatch.utils.net;

import okhttp3.OkHttpClient;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

public final class TestConnections {
    public static HtmlConnection html() {
        return new HtmlConnection(new OkHttpClient());
    }

    public static JsoupConnection jsoup() {
        return new JsoupConnection(html());
    }
}