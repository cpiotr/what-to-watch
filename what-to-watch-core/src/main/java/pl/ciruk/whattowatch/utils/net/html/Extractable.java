package pl.ciruk.whattowatch.utils.net.html;

import org.jsoup.nodes.Element;

public interface Extractable<T> {
    T extractFrom(Element element);
}
