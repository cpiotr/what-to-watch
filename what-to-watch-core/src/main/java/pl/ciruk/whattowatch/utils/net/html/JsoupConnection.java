package pl.ciruk.whattowatch.utils.net.html;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
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
    public Optional<Element> connectToAndGet(HttpUrl url) {
        return connection.connectToAndGet(url)
                .map(Jsoup::parse);
    }

    @Override
    public Optional<Element> connectToAndGet(HttpUrl url, String from, String to) {
        return connection.connectToAndGet(url)
                .map(contents -> getSubstring(contents, from, to))
                .map(Jsoup::parse);
    }

    private String getSubstring(String contents, String from, String to) {
        var beginIndex = contents.indexOf(from);
        if (beginIndex < 0) {
            return contents;
        }
        var endIndex = contents.indexOf(to, beginIndex);
        return (endIndex >= 0)
                ? contents.substring(beginIndex, endIndex + to.length())
                : contents.substring(beginIndex);
    }

    @Override
    public Optional<Element> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action) {
        return connection.connectToAndConsume(url, action)
                .map(Jsoup::parse);
    }

    @Override
    public Optional<Element> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action, Consumer<Response> responseConsumer) {
        return connection.connectToAndConsume(url, action, responseConsumer)
                .map(Jsoup::parse);
    }
}
