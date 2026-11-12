package pl.ciruk.whattowatch.boot.boundary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.filter.SuggestionFilter;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Each test uses its own titles, since found films stay cached for the whole context
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "w2w.bootstrap.enabled=false")
class EndpointsTest {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @MockitoBean(name = "titleProvider")
    private TitleProvider titleProvider;
    @MockitoBean(name = "filmwebDescriptions")
    private DescriptionProvider descriptionProvider;
    @MockitoBean(name = "imdbScores")
    private ScoresProvider imdbScores;
    @MockitoBean(name = "filmwebScores")
    private ScoresProvider filmwebScores;
    @MockitoBean(name = "metacriticScores")
    private ScoresProvider metacriticScores;
    @MockitoBean(name = "suggestionFilter")
    private SuggestionFilter suggestionFilter;

    @BeforeEach
    void setUp() {
        when(suggestionFilter.isWorthWatching(any())).thenReturn(true);
    }

    @Test
    void shouldSearchFilmsOfPageAtOnce() throws Exception {
        var first = title("First");
        var second = title("Second");
        when(titleProvider.streamOfTitles(TitleKind.FILM, 1)).thenAnswer(ignored -> Stream.of(first, second));
        var secondSearched = new CountDownLatch(1);
        when(descriptionProvider.findDescriptionBy(first)).thenAnswer(ignored ->
                secondSearched.await(5, SECONDS) ? Optional.of(descriptionOf(first)) : Optional.empty());
        when(descriptionProvider.findDescriptionBy(second)).thenAnswer(ignored -> {
            secondSearched.countDown();
            return Optional.of(descriptionOf(second));
        });

        var events = get("/resources/suggestions/1/stream").body();

        assertThat(events).contains("\"title\":\"First\"", "\"title\":\"Second\"", "poisonPill");
    }

    @Test
    void shouldListSuggestedFilms() throws Exception {
        var title = title("Listed");
        when(titleProvider.streamOfTitles(TitleKind.FILM, 2)).thenAnswer(ignored -> Stream.of(title));
        when(descriptionProvider.findDescriptionBy(title)).thenReturn(Optional.of(descriptionOf(title)));

        var response = get("/resources/suggestions/2");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"title\":\"Listed\"", "\"watched\":false");
    }

    // Air (2023) is one of the films I rated
    @Test
    void shouldMarkFilmsIWatched() throws Exception {
        var title = Title.builder().title("Air").year(2023).build();
        when(titleProvider.streamOfTitles(TitleKind.FILM, 5)).thenAnswer(ignored -> Stream.of(title));
        when(descriptionProvider.findDescriptionBy(title)).thenReturn(Optional.of(descriptionOf(title)));

        var response = get("/resources/suggestions/5");

        assertThat(response.body()).contains("\"title\":\"Air\"", "\"watched\":true");
    }

    @Test
    void shouldSuggestSeriesWhenAskedForThem() throws Exception {
        var series = Title.builder().title("Series").originalTitle("Series").year(2020).kind(TitleKind.SERIES).build();
        var film = title("Film");
        when(titleProvider.streamOfTitles(TitleKind.SERIES, 4)).thenAnswer(ignored -> Stream.of(series));
        when(titleProvider.streamOfTitles(TitleKind.FILM, 4)).thenAnswer(ignored -> Stream.of(film));
        when(descriptionProvider.findDescriptionBy(series)).thenReturn(Optional.of(descriptionOf(series)));
        when(descriptionProvider.findDescriptionBy(film)).thenReturn(Optional.of(descriptionOf(film)));

        var listed = get("/resources/suggestions/4?kind=SERIES").body();
        var streamed = get("/resources/suggestions/4/stream?kind=SERIES").body();

        assertThat(listed).contains("\"title\":\"Series\"").doesNotContain("\"title\":\"Film\"");
        assertThat(streamed).contains("\"title\":\"Series\"").doesNotContain("\"title\":\"Film\"");
    }

    @Test
    void shouldAnswerNotFoundForFilmWithoutDescription() throws Exception {
        var response = get("/resources/descriptions?title=Unknown&year=2020");

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldKeepErrorDetailsInLog() throws Exception {
        when(titleProvider.streamOfTitles(TitleKind.FILM, 3)).thenThrow(new IllegalStateException("Could not get https://internal.example"));

        var response = get("/resources/titles/3");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).isEqualTo("{\"error\":\"Internal error\"}");
    }

    @Test
    void shouldGiveScoresOfSitesThatAnswered() throws Exception {
        when(metacriticScores.findScoresBy(any())).thenAnswer(ignored -> Stream.of(Score.critic(0.81, 44)));
        when(filmwebScores.findScoresBy(any())).thenThrow(new IllegalStateException("Throttled"));

        var response = get("/resources/scores?title=Moana&year=2016");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"grade\":0.81");
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Title title(String text) {
        return Title.builder().title(text).originalTitle(text).year(2020).build();
    }

    private static Description descriptionOf(Title title) {
        return Description.builder().title(title).foundFor(title).build();
    }
}
