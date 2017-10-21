package pl.ciruk.core.net.html;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class JsoupConnection implements HttpConnection<Element> {

    private HttpConnection<String> connection;

    public JsoupConnection(HttpConnection<String> connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Element> connectToAndGet(String url) {
        return connection.connectToAndGet(url)
                .map(Jsoup::parse);
    }

    public Optional<Element> connectToAndConsume(String url, Consumer<Request.Builder> action) {
        return connection.connectToAndConsume(url, action)
                .map(Jsoup::parse);
    }
}
