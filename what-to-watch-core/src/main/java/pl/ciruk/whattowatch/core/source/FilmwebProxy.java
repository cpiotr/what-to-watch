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
                .addQueryParameter("startYear", String.valueOf(previous(year)))
                .addQueryParameter("endYear", String.valueOf(next(year)))
                .build();

        return Optional.of(url)
                .map(Object::toString)
                .flatMap(connection::connectToAndGet);
    }

    public Optional<Element> getPageWithFilmDetailsFor(String href) {
        return Optional.ofNullable(href)
                .map(this::resolveLink)
                .flatMap(connection::connectToAndGet);
    }

    public String resolveLink(String href) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(HOST)
                .build()
                .resolve(href);
        return url.toString();
    }

    private static int next(int year) {
        return year+1;
    }

    private static int previous(int year) {
        return year-1;
    }
}
