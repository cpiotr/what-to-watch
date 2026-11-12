package pl.ciruk.whattowatch.core.title.ekino;

import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

class EkinoTitleProviderTest {
    private static final String MIS = "Miś";

    private HttpConnection<Element> connection;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(HttpConnection.class);
    }

    @Test
    void shouldReadTitleWithoutQualityLabel() {
        givenPage(film("Pojednanie - HD", "Atonement", "2026 | Dramat"));

        assertThat(new EkinoTitleProvider(connection, 1).streamOfTitles(TitleKind.FILM, 1)).containsExactly(Title.builder()
                .title("Pojednanie")
                .originalTitle("Atonement")
                .year(2026)
                .url("https://ekino.ws/movie/show/film/1")
                .build());
    }

    @Test
    void shouldKeepHyphenInTitle() {
        givenPage(film("Spider-Man - HD", "Spider-Man", "2002 | Akcja"));

        assertThat(new EkinoTitleProvider(connection, 1).streamOfTitles(TitleKind.FILM, 1))
                .extracting(Title::localTitle)
                .containsExactly("Spider-Man");
    }

    @Test
    void shouldReadTitleWithoutLabelAndYearWithoutCategories() {
        givenPage(film(MIS, MIS, "1981"));

        assertThat(new EkinoTitleProvider(connection, 1).streamOfTitles(TitleKind.FILM, 1))
                .extracting(Title::localTitle, Title::year)
                .containsExactly(tuple(MIS, 1981));
    }

    @Test
    void shouldKeepTitleWithoutYear() {
        givenPage(film(MIS, MIS, "brak | Komedia"));

        assertThat(new EkinoTitleProvider(connection, 1).streamOfTitles(TitleKind.FILM, 1))
                .extracting(Title::year)
                .containsExactly(Title.MISSING_YEAR);
    }

    @Test
    void shouldStartRequestsAtFirstPage() {
        when(connection.get(any())).thenReturn(Optional.empty());

        assertThat(new EkinoTitleProvider(connection, 2).streamOfTitles(TitleKind.FILM, 2)).isEmpty();

        var urls = ArgumentCaptor.forClass(HttpUrl.class);
        verify(connection, times(2)).get(urls.capture());
        assertThat(urls.getAllValues()).extracting(HttpUrl::toString).containsExactly(
                "https://ekino.ws/movie/cat/strona%5B3%5D+",
                "https://ekino.ws/movie/cat/strona%5B4%5D+");
    }

    private void givenPage(String film) {
        when(connection.get(any())).thenReturn(Optional.of(Jsoup.parse(
                "<div class=\"mainWrap\">" + film + "</div>")));
    }

    // Shaped like a film on Ekino's list
    private static String film(String title, String originalTitle, String categories) {
        return "<div class=\"movies-list-item\"><div class=\"opis-list\">"
                + "<div class=\"title\"><a href=\"/movie/show/film/1\">" + title + "&nbsp;</a><br>"
                + "<span class=\"blue\"><a class=\"blue\" href=\"/movie/show/film/1\">" + originalTitle + "</a></span></div>"
                + "<div class=\"info-categories\"><p class=\"cates\">" + categories + "</p></div>"
                + "</div></div>";
    }
}
