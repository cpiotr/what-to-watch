package pl.ciruk.core.net;

import org.jsoup.Connection;
import org.jsoup.nodes.Element;

import java.util.Optional;
import java.util.function.Consumer;

public interface JsoupConnection {
    Optional<Element> connectToAndGet(String url);

    Optional<Element> connectToAndConsume(String url, Consumer<Connection> action);
}
