package pl.ciruk.whattowatch.core.source;

import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.time.LocalDate;
import java.util.Optional;

public class FilmwebProxy {
    private static final int CURRENT_YEAR = LocalDate.now().getYear();
    private static final String MIN_VOTES = String.valueOf(500);
    private final HttpConnection<Element> connection;

    public FilmwebProxy(HttpConnection<Element> connection) {
        this.connection = connection;
    }

    public Optional<Element> searchBy(String title, int year) {
        var url = createHttpBuilder()
                .addPathSegment("search")
                .addPathSegment("film")
                .addQueryParameter("q", title)
                .addQueryParameter("startYear", previous(year))
                .addQueryParameter("endYear", next(year))
                .addQueryParameter("startCount", MIN_VOTES)
                .build();

        return connection.connectToAndGet(url);
    }

    public Optional<Element> getPageWithFilmDetailsFor(String href) {
        return Optional.ofNullable(href)
                .map(this::resolveLink)
                .flatMap(connection::connectToAndGet);
    }

    public HttpUrl resolveLink(String href) {
        return createHttpBuilder()
                .build()
                .resolve(href);
    }

    private static HttpUrl.Builder createHttpBuilder() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("www.filmweb.pl");
    }

    private static String next(int year) {
        return String.valueOf(Math.min(year + 1, CURRENT_YEAR));
    }

    private static String previous(int year) {
        return String.valueOf(year - 1);
    }
}
