package pl.ciruk.whattowatch.core.source;

import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class FilmwebProxyTest {
    private static final String TITLE = "Incepcja";
    private static final long FILM_ID = 500_891;

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
        givenResponse("/api/v1/films/search", """
                {"total":2,"searchHits":[
                  {"id":500891,"type":"film","matchedTitle":"Incepcja"},
                  {"id":42,"type":"serial","matchedTitle":"Incepcja: serial"}
                ]}""");
        givenResponse("/api/v1/title/500891/info", """
                {"id":500891,"title":"Incepcja","originalTitle":"Inception","year":2010,"type":"film"}""");

        var films = filmwebProxy.searchBy(TITLE, 2010).toList();

        assertThat(films).containsExactly(new FilmwebFilm(FILM_ID, TITLE, "Inception", 2010));
    }

    @Test
    void shouldSearchAroundGivenYear() {
        givenResponse("/api/v1/films/search", """
                {"total":0,"searchHits":[]}""");

        assertThat(filmwebProxy.searchBy(TITLE, 2010)).isEmpty();

        verify(connection).get(argThat((HttpUrl url) ->
                TITLE.equals(url.queryParameter("query"))
                        && "2009".equals(url.queryParameter("startYear"))
                        && "2011".equals(url.queryParameter("endYear"))));
    }

    @Test
    void shouldFindNoFilmsWhenResponseIsNotJson() {
        givenResponse("/api/v1/films/search", "<html>Service unavailable</html>");

        assertThat(filmwebProxy.searchBy(TITLE, 2010)).isEmpty();
    }

    @Test
    void shouldParseOnlyFilmDetailsFromPage() {
        givenResponse("/film/Incepcja-2010-500891", """
                <html><body>
                <div class="page__group" data-group="g0">header</div>
                <div class="page__group" data-group="g1">details</div>
                <div class="page__group" data-group="g3">reviews</div>
                </body></html>""");

        var page = filmwebProxy.getPageWithFilmDetailsFor(new FilmwebFilm(FILM_ID, TITLE, "Inception", 2010));

        assertThat(page).map(Element::text).contains("details");
    }

    @Test
    void shouldLinkToFilmPage() {
        var film = new FilmwebFilm(FILM_ID, TITLE, "Inception", 2010);

        assertThat(filmwebProxy.linkTo(film).toString())
                .isEqualTo("https://www.filmweb.pl/film/Incepcja-2010-500891");
    }

    private void givenResponse(String path, String body) {
        when(connection.get(argThat((HttpUrl url) -> url != null && path.equals(url.encodedPath()))))
                .thenReturn(Optional.of(body));
    }
}
