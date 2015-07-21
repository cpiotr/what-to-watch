package pl.ciruk.core.net;

import com.squareup.okhttp.Request;
import org.jsoup.nodes.Element;

import java.util.Optional;
import java.util.function.Consumer;

public interface JsoupConnection {
    Optional<Element> connectToAndGet(String url);

    Optional<Element> connectToAndConsume(String url, Consumer<Request.Builder> action);
}
