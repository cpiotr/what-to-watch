package pl.ciruk.core.net;

import com.codahale.metrics.MetricRegistry;
import lombok.experimental.UtilityClass;
import okhttp3.OkHttpClient;
import pl.ciruk.core.net.html.JsoupConnection;

@UtilityClass
public final class TestConnections {
    public static HtmlConnection html() {
        return new HtmlConnection(new OkHttpClient(), new MetricRegistry());
    }

    public static JsoupConnection jsoup() {
        return new JsoupConnection(html());
    }
}