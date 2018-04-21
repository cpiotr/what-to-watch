package pl.ciruk.core.net.html;

import okhttp3.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.core.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.function.Consumer;

public class JsoupConnection implements HttpConnection<Element> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
