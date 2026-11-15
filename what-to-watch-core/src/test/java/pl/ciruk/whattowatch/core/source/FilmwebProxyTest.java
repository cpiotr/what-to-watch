package pl.ciruk.whattowatch.core.source;

import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.core.title.TitleKind;

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
    void shouldSearchAroundGivenYear() {
        givenResponse(FILMS_SEARCH, """
                {"total":0,"searchHits":[]}""");

        assertThat(filmwebProxy.searchBy(TITLE, 2010, TitleKind.FILM)).isEmpty();

        verify(connection).get(argThat((HttpUrl url) ->
                TITLE.equals(url.queryParameter("query"))
                        && "2009".equals(url.queryParameter("startYear"))
                        && "2011".equals(url.queryParameter("endYear"))));
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

    private void givenResponse(String path, String body) {
        when(connection.get(argThat((HttpUrl url) -> url != null && path.equals(url.encodedPath()))))
                .thenReturn(Optional.of(body));
    }
}
