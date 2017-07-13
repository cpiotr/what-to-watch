package pl.ciruk.core.net;

import com.codahale.metrics.MetricRegistry;
import okhttp3.OkHttpClient;
import pl.ciruk.core.net.html.JsoupConnection;

public class Connections {
    public static HtmlConnection html() {
        return new HtmlConnection(new OkHttpClient(), new MetricRegistry());
    }

    public static JsoupConnection jsoup() {
        return new JsoupConnection(html());
    }
}