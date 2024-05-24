package pl.ciruk.whattowatch.utils.net.html;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class JsoupConnection implements HttpConnection<Element> {
    private final HttpConnection<String> connection;
    private final Playwright playwright;
    private final Browser browser;
    private final ExecutorService threadPool;
    private final ThreadLocal<Browser> browserThreadLocal;

    public JsoupConnection(HttpConnection<String> connection) {
        this.connection = connection;
        playwright = Playwright.create();
        browser = playwright.webkit().launch();
        threadPool = Executors.newFixedThreadPool(32);
        browserThreadLocal = ThreadLocal.withInitial(() -> Playwright.create().webkit().launch());
    }

    @Override
    public Optional<Element> connectToAndGet(HttpUrl url) {
        return connection.connectToAndGet(url)
                .map(this::parseContent);
    }

    private Document parseContent(String html) {
        return CompletableFuture.supplyAsync(() -> {
            final var browser = browserThreadLocal.get();
            try (final var page = browser.newPage()) {
                page.setContent(html);
                return Jsoup.parse(page.content());
            }
        }, threadPool).join();
    }

    @Override
    public Optional<Element> connectToAndGet(HttpUrl url, String from, String to) {
        return connection.connectToAndGet(url)
                .map(contents -> getSubstring(contents, from, to))
                .map(this::parseContent);
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
                .map(this::parseContent);
    }

    @Override
    public Optional<Element> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action, Consumer<Response> responseConsumer) {
        return connection.connectToAndConsume(url, action, responseConsumer)
                .map(this::parseContent);
    }
}
