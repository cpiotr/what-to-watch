package pl.ciruk.core.net;

import com.codahale.metrics.MetricRegistry;
import com.squareup.okhttp.OkHttpClient;
import pl.ciruk.core.net.html.JsoupConnection;

public class Connections {
    public static HtmlConnection html() {
        return new HtmlConnection(OkHttpClient::new, new MetricRegistry());
    }

    public static JsoupConnection jsoup() {
        return new JsoupConnection(html());
    }
}