package pl.ciruk.whattowatch.source;

import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;

import java.util.Optional;

public class FilmwebProxy {
    private final HttpConnection<Element> connection;

    public FilmwebProxy(HttpConnection<Element> connection) {
        this.connection = connection;
    }

    public Optional<Element> searchFor(String title, int year) {
        var url = new HttpUrl.Builder()
                .scheme("http")
                .host("www.filmweb.pl")
                .addPathSegment("search/film")
                .addQueryParameter("q", title)
                .addQueryParameter("startYear", String.valueOf(year))
                .addQueryParameter("endYear", String.valueOf(year))
                .build();

        return Optional.of(url)
                .map(Object::toString)
                .flatMap(connection::connectToAndGet);
    }

    public Optional<Element> getPageWithFilmDetailsFor(String href) {
        var url = new HttpUrl.Builder()
                .scheme("http")
                .host("www.filmweb.pl")
                .build()
                .resolve(href);

        return Optional.ofNullable(url)
                .map(Object::toString)
                .flatMap(connection::connectToAndGet);
    }
}
