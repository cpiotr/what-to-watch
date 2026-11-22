package pl.ciruk.whattowatch.core.source;

import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleKind;

import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class FilmwebProxyTest {
    private static final String FILMS_SEARCH = "/api/v1/films/search";
    private static final String TITLE = "Incepcja";
    private static final long FILM_ID = 500_891;
    private static final String SERIES = "Wednesday";
    private static final long SERIES_ID = 10_007;
    private static final String TOP_GEAR = "Top Gear";
    private static final String AIR = "Air";

    private HttpConnection<String> connection;
    private FilmwebProxy filmwebProxy;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(HttpConnection.class);
        filmwebProxy = new FilmwebProxy(connection);
    }

    @Test
    void shouldFindFilmsInSearchResults() {
        givenResponse(FILMS_SEARCH, """
                {"total":2,"searchHits":[
                  {"id":500891,"type":"film","matchedTitle":"Incepcja"},
                  {"id":42,"type":"serial","matchedTitle":"Incepcja: serial"}
                ]}""");
        givenResponse("/api/v1/title/500891/info", """
                {"id":500891,"title":"Incepcja","originalTitle":"Inception","year":2010,"type":"film"}""");

        var films = filmwebProxy.searchBy(TITLE, 2010, TitleKind.FILM).toList();

        assertThat(films).containsExactly(new FilmwebTitle(FILM_ID, TITLE, "Inception", 2010, TitleKind.FILM));
    }

    @Test
    void shouldLookUpOnlyHitsNamedLikeQuery() {
        givenResponse(FILMS_SEARCH, """
                {"total":3,"searchHits":[
                  {"id":10042518,"type":"film","matchedTitle":"Squealer - The Serial Killer"},
                  {"id":10002852,"type":"film","matchedTitle":"The Killer"},
                  {"id":10043408,"type":"film","matchedTitle":"The Lady Killer"}
                ]}""");
        givenResponse("/api/v1/title/10002852/info", """
                {"id":10002852,"title":"Zabójca","originalTitle":"The Killer","year":2023,"type":"film"}""");

        var films = filmwebProxy.searchBy("The Killer", 2023, TitleKind.FILM).toList();

        assertThat(films).containsExactly(new FilmwebTitle(10_002_852, "Zabójca", "The Killer", 2023, TitleKind.FILM));
        verify(connection, never()).get(argThat((HttpUrl url) -> url != null && url.encodedPath().contains("10042518")));
        verify(connection, never()).get(argThat((HttpUrl url) -> url != null && url.encodedPath().contains("10043408")));
    }

    @Test
    void shouldFindSeriesInSeriesSearchResults() {
        givenResponse("/api/v1/serials/search", """
                {"total":1,"searchHits":[{"id":10007,"type":"serial","matchedTitle":"Wednesday"}]}""");
        givenResponse("/api/v1/title/10007/info", """
                {"id":10007,"title":"Wednesday","originalTitle":"Wednesday","year":2022,"otherYear":2026,"type":"serial"}""");

        var series = filmwebProxy.searchBy(SERIES, 2022, TitleKind.SERIES).toList();

        assertThat(series).containsExactly(new FilmwebTitle(SERIES_ID, SERIES, SERIES, 2022, TitleKind.SERIES));
    }

    @Test
    void shouldFindTvProgrammeWhenNoSeriesIsFound() {
        givenResponse("/api/v1/serials/search", """
                {"total":0,"searchHits":[]}""");
        givenResponse("/api/v1/tvshows/search", """
                {"total":1,"searchHits":[{"id":770749,"type":"tvshow","matchedTitle":"Top Gear"}]}""");
        givenResponse("/api/v1/title/770749/info", """
                {"id":770749,"title":"Top Gear","originalTitle":"Top Gear","year":2002,"type":"tvshow"}""");

        var series = filmwebProxy.searchBy(TOP_GEAR, 2002, TitleKind.SERIES).toList();

        assertThat(series).containsExactly(new FilmwebTitle(770_749, TOP_GEAR, TOP_GEAR, 2002, TitleKind.SERIES, "tvshow"));
    }

    @Test
    void shouldNotSearchTvProgrammesForFilms() {
        givenResponse(FILMS_SEARCH, """
                {"total":0,"searchHits":[]}""");

        assertThat(filmwebProxy.searchBy(TOP_GEAR, 2002, TitleKind.FILM)).isEmpty();

        verify(connection, never()).get(argThat((HttpUrl url) -> url != null && url.encodedPath().contains("tvshows")));
    }

    // Filmweb found it by the name streaming services use, "Marvel's Daredevil", which isn't its own title
    @Test
    void shouldKeepTitleFoundByAnotherName() {
        givenResponse("/api/v1/serials/search", """
                {"total":1,"searchHits":[{"id":702252,"type":"serial","matchedTitle":"Marvel's Daredevil"}]}""");
        givenResponse("/api/v1/title/702252/info", """
                {"id":702252,"title":"Daredevil","originalTitle":"Daredevil","year":2015,"type":"serial"}""");
        var title = Title.builder().originalTitle("Marvel's Daredevil").year(2015).kind(TitleKind.SERIES).build();

        var series = filmwebProxy.searchFor("Marvel's Daredevil", title);

        assertThat(series).containsExactly(new FilmwebTitle(702_252, "Daredevil", "Daredevil", 2015, TitleKind.SERIES));
    }

    @Test
    void shouldPreferTitlesNamedLikeSearchedOne() {
        givenResponse(FILMS_SEARCH, """
                {"total":2,"searchHits":[
                  {"id":1,"type":"film","matchedTitle":"Air"},
                  {"id":10030286,"type":"film","matchedTitle":"Air"}
                ]}""");
        givenResponse("/api/v1/title/1/info", """
                {"id":1,"title":"Powietrze","originalTitle":"Vozdukh","year":2023,"type":"film"}""");
        givenResponse("/api/v1/title/10030286/info", """
                {"id":10030286,"title":"Air","originalTitle":"Air","year":2023,"type":"film"}""");
        var title = Title.builder().originalTitle(AIR).year(2023).build();

        var films = filmwebProxy.searchFor(AIR, title);

        assertThat(films).containsExactly(new FilmwebTitle(10_030_286, AIR, AIR, 2023, TitleKind.FILM));
    }

    @Test
    void shouldSearchAroundGivenYear() {
        givenResponse(FILMS_SEARCH, """
                {"total":0,"searchHits":[]}""");

        assertThat(filmwebProxy.searchBy(TITLE, 2010, TitleKind.FILM)).isEmpty();

        verify(connection).get(argThat((HttpUrl url) ->
                TITLE.equals(url.queryParameter("query"))
                        && "2009".equals(url.queryParameter("startYear"))
                        && "2011".equals(url.queryParameter("endYear"))));
    }

    // Filmweb ranks "Air" (2023) on the third of four pages, after films such as "Air Fryer Slaughter"
    @Test
    void shouldFindFilmOnLaterPageOfSearchResults() {
        givenSearchPage(null, """
                {"total":21,"searchHits":[
                  {"id":1,"type":"film","matchedTitle":"Air Fryer Slaughter"},
                  {"id":2,"type":"film","matchedTitle":"On Air"}
                ]}""");
        givenSearchPage("2", """
                {"total":21,"searchHits":[{"id":3,"type":"film","matchedTitle":"Love in the Air"}]}""");
        givenSearchPage("3", """
                {"total":21,"searchHits":[{"id":10030286,"type":"film","matchedTitle":"Air"}]}""");
        givenResponse("/api/v1/title/10030286/info", """
                {"id":10030286,"title":"Air","originalTitle":"Air","year":2023,"type":"film"}""");

        var films = filmwebProxy.searchBy(AIR, 2023, TitleKind.FILM).toList();

        assertThat(films).containsExactly(new FilmwebTitle(10_030_286, AIR, AIR, 2023, TitleKind.FILM));
    }

    @Test
    void shouldReadAtMostFivePagesOfSearchResults() {
        givenSearchPage(null, """
                {"total":1000,"searchHits":[{"id":1,"type":"film","matchedTitle":"Love Actually"}]}""");

        assertThat(filmwebProxy.searchBy("Love", 2003, TitleKind.FILM)).isEmpty();

        verify(connection).get(argThat((HttpUrl url) -> url != null && "5".equals(url.queryParameter("page"))));
        verify(connection, never()).get(argThat((HttpUrl url) -> url != null && "6".equals(url.queryParameter("page"))));
    }

    @Test
    void shouldFindNoFilmsWhenResponseIsNotJson() {
        givenResponse(FILMS_SEARCH, "<html>Service unavailable</html>");

        assertThat(filmwebProxy.searchBy(TITLE, 2010, TitleKind.FILM)).isEmpty();
    }

    @Test
    void shouldParseOnlyFilmDetailsFromPage() {
        givenResponse("/film/Incepcja-2010-500891", """
                <html><body>
                <div class="page__group" data-group="g0">header</div>
                <div class="page__group" data-group="g1">details</div>
                <div class="page__group" data-group="g3">reviews</div>
                </body></html>""");

        var page = filmwebProxy.getDetailsPageFor(new FilmwebTitle(FILM_ID, TITLE, "Inception", 2010, TitleKind.FILM));

        assertThat(page).map(Element::text).contains("details");
    }

    @Test
    void shouldLinkToFilmPage() {
        var film = new FilmwebTitle(FILM_ID, TITLE, "Inception", 2010, TitleKind.FILM);

        assertThat(filmwebProxy.linkTo(film).toString())
                .isEqualTo("https://www.filmweb.pl/film/Incepcja-2010-500891");
    }

    @Test
    void shouldLinkToSeriesPage() {
        var series = new FilmwebTitle(SERIES_ID, SERIES, SERIES, 2022, TitleKind.SERIES);

        assertThat(filmwebProxy.linkTo(series).toString())
                .isEqualTo("https://www.filmweb.pl/serial/Wednesday-2022-10007");
    }

    @Test
    void shouldLinkToTvProgrammePage() {
        var programme = new FilmwebTitle(770_749, TOP_GEAR, TOP_GEAR, 2002, TitleKind.SERIES, "tvshow");

        assertThat(filmwebProxy.linkTo(programme).toString())
                .isEqualTo("https://www.filmweb.pl/tvshow/Top+Gear-2002-770749");
    }

    @Test
    void shouldLinkWithoutSlashInTitle() {
        var series = new FilmwebTitle(850_114, "Sex/Life", "Sex/Life", 2021, TitleKind.SERIES);

        assertThat(filmwebProxy.linkTo(series).toString())
                .isEqualTo("https://www.filmweb.pl/serial/SexLife-2021-850114");
    }

    // The first page has no page number
    private void givenSearchPage(String page, String body) {
        when(connection.get(argThat((HttpUrl url) -> url != null
                && FILMS_SEARCH.equals(url.encodedPath())
                && Objects.equals(page, url.queryParameter("page")))))
                .thenReturn(Optional.of(body));
    }

    private void givenResponse(String path, String body) {
        when(connection.get(argThat((HttpUrl url) -> url != null && path.equals(url.encodedPath()))))
                .thenReturn(Optional.of(body));
    }
}
