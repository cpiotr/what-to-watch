package pl.ciruk.whattowatch.core.score.metacritic;

import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.util.Optional;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class MetacriticScoresProviderTest {

    private HttpConnection<Element> connection;
    private MetacriticScoresProvider provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(HttpConnection.class);
        when(connection.connectToAndGet(any(HttpUrl.class))).thenReturn(Optional.empty());
        provider = new MetacriticScoresProvider(connection, Executors.newSingleThreadExecutor());
    }

    @Test
    void shouldReadMetascoreFromFilmPage() {
        givenPage("/movie/moana/", filmPage("Moana", "2016-11-23", 81, 44));

        var scores = provider.findScoresBy(descriptionOf("Moana", 2016)).toList();

        assertThat(scores).containsExactly(Score.builder()
                .grade(0.81)
                .quantity(44)
                .source("Metacritic")
                .type(ScoreType.CRITIC)
                .url("https://www.metacritic.com/movie/moana/")
                .build());
    }

    @Test
    void shouldFindLaterFilmWithSameTitleByYearInLink() {
        givenPage("/movie/little-women/", filmPage("Little Women", "1994-12-21", 87, 23));
        givenPage("/movie/little-women-2019/", filmPage("Little Women", "2019-12-25", 91, 57));

        var scores = provider.findScoresBy(descriptionOf("Little Women", 2019)).toList();

        assertThat(scores).extracting(Score::url).containsExactly("https://www.metacritic.com/movie/little-women-2019/");
    }

    @Test
    void shouldIgnoreFilmFromDifferentYear() {
        givenPage("/movie/west-side-story/", filmPage("West Side Story", "1961-12-13", 86, 17));

        assertThat(provider.findScoresBy(descriptionOf("West Side Story", 2021))).isEmpty();
    }

    @Test
    void shouldIgnoreFilmWithoutMetascore() {
        givenPage("/movie/moana/", Jsoup.parse("""
                <script type="application/ld+json">{"@type":"Movie","name":"Moana","datePublished":"2016-11-23"}</script>"""));

        assertThat(provider.findScoresBy(descriptionOf("Moana", 2016))).isEmpty();
    }

    @Test
    void shouldBuildLinkFromTitle() {
        assertThat(MetacriticScoresProvider.linkFor("Fahrenheit 9/11")).isEqualTo("fahrenheit-911");
        assertThat(MetacriticScoresProvider.linkFor("T2: Trainspotting")).isEqualTo("t2-trainspotting");
        assertThat(MetacriticScoresProvider.linkFor("Amélie")).isEqualTo("amelie");
        assertThat(MetacriticScoresProvider.linkFor("The Boy, the Mole, the Fox and the Horse"))
                .isEqualTo("the-boy-the-mole-the-fox-and-the-horse");
    }

    private void givenPage(String path, Element page) {
        when(connection.connectToAndGet(argThat((HttpUrl url) -> url != null && path.equals(url.encodedPath()))))
                .thenReturn(Optional.of(page));
    }

    private static Element filmPage(String name, String releaseDate, int metascore, int numberOfReviews) {
        var filmData = "{\"@type\":\"Movie\",\"name\":\"%s\",\"datePublished\":\"%s\",\"aggregateRating\":{\"ratingValue\":%d,\"reviewCount\":%d}}"
                .formatted(name, releaseDate, metascore, numberOfReviews);
        return Jsoup.parse("<html><head><script type=\"application/ld+json\">" + filmData + "</script></head><body></body></html>");
    }

    private static Description descriptionOf(String title, int year) {
        return Description.builder()
                .title(Title.builder().title(title).year(year).build())
                .build();
    }
}
