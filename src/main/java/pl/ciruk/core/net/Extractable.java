package pl.ciruk.core.net;

import org.jsoup.nodes.Element;

import java.util.Optional;

public interface Extractable {
    Optional<String> extractFrom(Element element);
}
