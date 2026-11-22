package pl.ciruk.whattowatch.core.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.utils.text.Texts;

import java.lang.invoke.MethodHandles;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.function.Predicate.not;

public class FilmwebProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // The film's details are between these groups, so jsoup doesn't parse the rest of the page
    private static final String DETAILS_START = "<div class=\"page__group\" data-group=\"g1\">";
    private static final String DETAILS_END = "<div class=\"page__group\" data-group=\"g3\">";
    // Enough for a short title: Filmweb ranks "Air" (2023) third of four pages, after other films with "air" in them
    private static final int MOST_SEARCH_PAGES = 5;
    private static final int FIRST_PAGE = 1;

    private final HttpConnection<String> connection;

    public FilmwebProxy(HttpConnection<String> connection) {
        this.connection = connection;
    }

    // The search page renders its results in the browser, so search through the API it uses
    // A series can be a TV programme to Filmweb, which only its search for programmes finds, so that search is next
    public Stream<FilmwebTitle> searchBy(String title, int year, TitleKind kind) {
        return typesOf(kind).stream()
                .map(type -> searchBy(title, year, kind, type))
                .filter(not(List::isEmpty))
                .findFirst()
                .orElse(List.of())
                .stream();
    }

    private List<FilmwebTitle> searchBy(String title, int year, TitleKind kind, String type) {
        return searchPages(title, year, type)
                .flatMap(searchResults -> StreamSupport.stream(searchResults.path("searchHits").spliterator(), false))
                .filter(hit -> type.equals(hit.path("type").asText()))
                // The hits are mostly other films whose titles contain the query, and each costs a request to look
                // up, so only those named like the query are. The search's years already match the film's.
                .filter(hit -> Texts.matchesAlphanumerically(hit.path("matchedTitle").asText(), title))
                .map(hit -> hit.path("id").asLong())
                .map(id -> findTitleBy(id, kind, type))
                .flatMap(Optional::stream)
                .toList();
    }

    // The search answers a page of hits at a time, and says how many there are in all
    private Stream<JsonNode> searchPages(String title, int year, String type) {
        var firstPage = searchPage(title, year, type, FIRST_PAGE);
        if (firstPage.isEmpty()) {
            return Stream.empty();
        }
        var hitsPerPage = firstPage.get().path("searchHits").size();
        var pages = hitsPerPage == 0 ? 1 : Math.ceilDiv(firstPage.get().path("total").asInt(), hitsPerPage);
        return Stream.concat(
                firstPage.stream(),
                IntStream.rangeClosed(FIRST_PAGE + 1, Math.min(pages, MOST_SEARCH_PAGES))
                        .mapToObj(page -> searchPage(title, year, type, page))
                        .flatMap(Optional::stream));
    }

    // The first page's link has no page number, as before there were more pages, so pages cached then still count
    private Optional<JsonNode> searchPage(String title, int year, String type, int page) {
        var builder = createHttpBuilder()
                .addPathSegments("api/v1")
                .addPathSegment(type + "s")
                .addPathSegment("search")
                .addQueryParameter("query", title)
                .addQueryParameter("startYear", previous(year))
                .addQueryParameter("endYear", next(year));
        if (page > FIRST_PAGE) {
            builder.addQueryParameter("page", String.valueOf(page));
        }
        var url = builder.build();

        return connection.get(url).flatMap(json -> readJson(json, url));
    }

    // Titles can share an original title and year, as Netflix's "The Diplomat" (2023) and a British series do, and
    // then their local titles tell them apart: "Dyplomatka" and "Dyplomatki"
    // Filmweb also finds a title by its other names, such as "Marvel's Daredevil" for "Daredevil", and by the years it
    // ran, such as 1971 for "Columbo" from 1968, so a title whose own name and year differ is still the one searched for
    public List<FilmwebTitle> searchFor(String query, Title title) {
        var found = searchBy(query, title.year(), title.kind()).toList();
        var matches = found.stream()
                .filter(film -> film.toTitle().matches(title))
                .toList();
        var exactMatches = matches.stream()
                .filter(film -> film.toTitle().matchesExactly(title))
                .toList();
        if (!exactMatches.isEmpty()) {
            return exactMatches;
        }
        return matches.isEmpty() ? found : matches;
    }

    // Filmweb calls a series a serial, and has TV programmes, such as "Top Gear", as a type of their own
    static List<String> typesOf(TitleKind kind) {
        return switch (kind) {
            case FILM -> List.of("film");
            case SERIES -> List.of("serial", "tvshow");
        };
    }

    private Optional<FilmwebTitle> findTitleBy(long id, TitleKind kind, String type) {
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
                        kind,
                        type));
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

    // Filmweb finds a film by the id at the end of the link and redirects any other title or year to the right one,
    // but not a title with a slash, such as "Sex/Life", whose encoded slash leads to its "not found" page
    public HttpUrl linkTo(FilmwebTitle film) {
        return createHttpBuilder()
                .addPathSegment(film.type())
                .addPathSegment(film.title().replace("/", "").replace(' ', '+') + "-" + film.year() + "-" + film.id())
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
