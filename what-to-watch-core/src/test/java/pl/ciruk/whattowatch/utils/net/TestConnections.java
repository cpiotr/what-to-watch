package pl.ciruk.whattowatch.utils.net;

import okhttp3.OkHttpClient;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.List;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class TestConnections {
    private TestConnections() {
        throw new AssertionError();
    }

    public static HtmlConnection html() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .cookieJar(new InMemoryCookieJar())
                .addInterceptor(new BackoffInterceptor())
                .build();

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
        return new HtmlConnection(httpClient, List.of(), List.of(new CloudflareBypass(httpClient, engine)));
    }

    public static JsoupConnection jsoup() {
        return new JsoupConnection(html());
    }

}
