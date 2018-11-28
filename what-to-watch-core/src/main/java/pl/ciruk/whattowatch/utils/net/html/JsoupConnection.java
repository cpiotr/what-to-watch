package pl.ciruk.whattowatch.utils.net.html;

import okhttp3.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.util.Optional;
import java.util.function.Consumer;

public class JsoupConnection implements HttpConnection<Element> {
    private final HttpConnection<String> connection;

    public JsoupConnection(HttpConnection<String> connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Element> connectToAndGet(String url) {
        return connection.connectToAndGet(url)
                .map(Jsoup::parse);
    }

    @Override
    public Optional<Element> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        return connection.connectToAndConsume(url, action)
                .map(Jsoup::parse);
    }
}
