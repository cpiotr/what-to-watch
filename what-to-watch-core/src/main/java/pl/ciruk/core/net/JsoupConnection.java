package pl.ciruk.core.net;

import org.jsoup.Connection;
import org.jsoup.nodes.Element;

import java.util.Optional;

public interface JsoupConnection {
    Optional<Element> connectToAndGet(String url);

    Connection to(String url);
}
