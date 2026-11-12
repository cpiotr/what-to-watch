package pl.ciruk.whattowatch.core.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.time.Year;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FilmwebProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // The film's details are between these groups, so jsoup doesn't parse the rest of the page
    private static final String DETAILS_START = "<div class=\"page__group\" data-group=\"g1\">";
    private static final String DETAILS_END = "<div class=\"page__group\" data-group=\"g3\">";

    private final HttpConnection<String> connection;

    public FilmwebProxy(HttpConnection<String> connection) {
        this.connection = connection;
    }

    // The search page renders its results in the browser, so search through the API it uses
    public Stream<FilmwebTitle> searchBy(String title, int year, TitleKind kind) {
        var url = createHttpBuilder()
                .addPathSegments("api/v1")
                .addPathSegment(typeOf(kind) + "s")
                .addPathSegment("search")
                .addQueryParameter("query", title)
                .addQueryParameter("startYear", previous(year))
                .addQueryParameter("endYear", next(year))
                .build();

        return connection.get(url)
                .flatMap(json -> readJson(json, url))
                .stream()
                .flatMap(searchResults -> StreamSupport.stream(searchResults.path("searchHits").spliterator(), false))
                .filter(hit -> typeOf(kind).equals(hit.path("type").asText()))
                .map(hit -> hit.path("id").asLong())
                .map(id -> findTitleBy(id, kind))
                .flatMap(Optional::stream);
    }

    // Filmweb calls a series a serial, in its search API, search results and links
    private static String typeOf(TitleKind kind) {
        return switch (kind) {
            case FILM -> "film";
            case SERIES -> "serial";
        };
    }

    private Optional<FilmwebTitle> findTitleBy(long id, TitleKind kind) {
        var url = createHttpBuilder()
                .addPathSegments("api/v1/title")
                .addPathSegment(String.valueOf(id))
                .addPathSegment("info")
                .build();

        return connection.get(url)
                .flatMap(json -> readJson(json, url))
                .map(info -> new FilmwebTitle(
                        id,
                        info.path("title").asText(),
                        info.path("originalTitle").asText(""),
                        info.path("year").asInt(),
                        kind));
    }

    private static Optional<JsonNode> readJson(String json, HttpUrl url) {
        try {
            return Optional.of(OBJECT_MAPPER.readTree(json));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Could not parse JSON from {}", url, e);
            return Optional.empty();
        }
    }

    public Optional<Element> getDetailsPageFor(FilmwebTitle film) {
        return connection.get(linkTo(film))
                .map(page -> Jsoup.parse(detailsOf(page)));
    }

    private static String detailsOf(String page) {
        var start = page.indexOf(DETAILS_START);
        if (start < 0) {
            return page;
        }
        var end = page.indexOf(DETAILS_END, start);
        return end < 0
                ? page.substring(start)
                : page.substring(start, end + DETAILS_END.length());
    }

    // Filmweb finds a film by the id at the end of the link and redirects any other title or year to the right one
    public HttpUrl linkTo(FilmwebTitle film) {
        return createHttpBuilder()
                .addPathSegment(typeOf(film.kind()))
                .addPathSegment(film.title().replace(' ', '+') + "-" + film.year() + "-" + film.id())
                .build();
    }

    private static HttpUrl.Builder createHttpBuilder() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("www.filmweb.pl");
    }

    private static String next(int year) {
        return String.valueOf(Math.min(year + 1, Year.now().getValue()));
    }

    private static String previous(int year) {
        return String.valueOf(year - 1);
    }
}
