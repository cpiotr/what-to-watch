package pl.ciruk.core.net;

import org.jsoup.nodes.Element;

import java.util.Optional;
import java.util.function.Function;

public interface Extractable<T> {
    T extractFrom(Element element);
}
