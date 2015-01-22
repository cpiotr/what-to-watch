package pl.ciruk.films.whattowatch.net;

import org.jsoup.nodes.Element;

public interface Extractable<T> {
    T extractFrom(Element element);
}
