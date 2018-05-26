package pl.ciruk.whattowatch.utils.net;

import org.jsoup.nodes.Element;

public interface Extractable<T> {
    T extractFrom(Element element);
}
