package pl.ciruk.whattowatch.utils.net.html;

import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.util.Optional;

public class JsoupConnection implements HttpConnection<Element> {
    private final HttpConnection<String> connection;

    public JsoupConnection(HttpConnection<String> connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Element> get(HttpUrl url) {
        return connection.get(url)
                .map(Jsoup::parse);
    }

    @Override
    public Optional<Element> post(HttpUrl url, RequestBody body) {
        return connection.post(url, body)
                .map(Jsoup::parse);
    }
}
