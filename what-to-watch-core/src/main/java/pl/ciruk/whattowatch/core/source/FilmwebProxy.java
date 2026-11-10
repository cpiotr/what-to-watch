package pl.ciruk.whattowatch.core.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FilmwebProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int CURRENT_YEAR = LocalDate.now().getYear();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final HttpConnection<Element> pageConnection;
    private final HttpConnection<String> apiConnection;

    public FilmwebProxy(HttpConnection<Element> pageConnection, HttpConnection<String> apiConnection) {
        this.pageConnection = pageConnection;
        this.apiConnection = apiConnection;
    }

    // The search page renders its results in the browser, so search through the API it uses
    public Stream<FilmwebFilm> searchBy(String title, int year) {
        var url = createHttpBuilder()
                .addPathSegments("api/v1/films/search")
                .addQueryParameter("query", title)
                .addQueryParameter("startYear", previous(year))
                .addQueryParameter("endYear", next(year))
                .build();

        return apiConnection.connectToAndGet(url)
                .flatMap(json -> readJson(json, url))
                .stream()
                .flatMap(searchResults -> StreamSupport.stream(searchResults.path("searchHits").spliterator(), false))
                .filter(hit -> "film".equals(hit.path("type").asText()))
                .map(hit -> hit.path("id").asLong())
                .map(this::findFilmBy)
                .flatMap(Optional::stream);
    }

    private Optional<FilmwebFilm> findFilmBy(long id) {
        var url = createHttpBuilder()
                .addPathSegments("api/v1/title")
                .addPathSegment(String.valueOf(id))
                .addPathSegment("info")
                .build();

        return apiConnection.connectToAndGet(url)
                .flatMap(json -> readJson(json, url))
                .map(info -> new FilmwebFilm(
                        id,
                        info.path("title").asText(),
                        info.path("originalTitle").asText(""),
                        info.path("year").asInt()));
    }

    private static Optional<JsonNode> readJson(String json, HttpUrl url) {
        try {
            return Optional.of(OBJECT_MAPPER.readTree(json));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Could not parse JSON from {}", url, e);
            return Optional.empty();
        }
    }

    public Optional<Element> getPageWithFilmDetailsFor(FilmwebFilm film) {
        return pageConnection.connectToAndGet(linkTo(film), "<div class=\"page__group\" data-group=\"g1\">", "<div class=\"page__group\" data-group=\"g3\">");
    }

    // Filmweb finds a film by the id at the end of the link and redirects any other title or year to the right one
    public HttpUrl linkTo(FilmwebFilm film) {
        return createHttpBuilder()
                .addPathSegment("film")
                .addPathSegment(film.title().replace(' ', '+') + "-" + film.year() + "-" + film.id())
                .build();
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
