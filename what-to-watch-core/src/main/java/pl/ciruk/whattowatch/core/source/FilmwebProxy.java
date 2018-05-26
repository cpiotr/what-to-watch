package pl.ciruk.whattowatch.core.source;

import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.util.Optional;

public class FilmwebProxy {
    public static final String HOST = "www.filmweb.pl";
    private final HttpConnection<Element> connection;

    public FilmwebProxy(HttpConnection<Element> connection) {
        this.connection = connection;
    }

    public Optional<Element> searchFor(String title, int year) {
        var url = new HttpUrl.Builder()
                .scheme("http")
                .host(HOST)
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
                .host(HOST)
                .build()
                .resolve(href);

        return Optional.ofNullable(url)
                .map(Object::toString)
                .flatMap(connection::connectToAndGet);
    }
}
