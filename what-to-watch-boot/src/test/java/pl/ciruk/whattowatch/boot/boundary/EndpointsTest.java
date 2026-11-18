package pl.ciruk.whattowatch.boot.boundary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
import java.nio.file.Files;
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
    private static final String RATINGS = "/resources/ratings";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;
    @Value("${local.management.port}")
    private int managementPort;

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

    // My own ratings change whenever I rate in the app, so the tests rate in a file of their own
    @DynamicPropertySource
    static void ratingsInTemporaryFile(DynamicPropertyRegistry registry) throws IOException {
        var directory = Files.createTempDirectory("ratings");
        var file = directory.resolve("rated-films.csv");
        Files.writeString(file, """
                title;originalTitle;year;rating;filmweb;filmwebVotes;metacritic;metacriticReviews;kind
                Air;;2023;7;7.01;30318;73;59;FILM
                """);
        directory.toFile().deleteOnExit();
        file.toFile().deleteOnExit();
        registry.add("w2w.ratings.file", file::toString);
    }

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
    void shouldEndFailedPageWithoutSayingItIsDone() throws Exception {
        when(titleProvider.streamOfTitles(TitleKind.FILM, 7)).thenThrow(new IllegalStateException("JustWatch is down"));

        var events = get("/resources/suggestions/7/stream").body();

        assertThat(events).doesNotContain("poisonPill");
    }

    @Test
    void shouldListSuggestedFilms() throws Exception {
        var title = title("Listed");
        when(titleProvider.streamOfTitles(TitleKind.FILM, 2)).thenAnswer(ignored -> Stream.of(title));
        when(descriptionProvider.findDescriptionBy(title)).thenReturn(Optional.of(descriptionOf(title)));

        var response = get("/resources/suggestions/2");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"title\":\"Listed\"", "\"myRating\":null");
    }

    // Air (2023) is the film rated in the test ratings
    @Test
    void shouldMarkFilmsIWatched() throws Exception {
        var title = Title.builder().title("Air").year(2023).build();
        when(titleProvider.streamOfTitles(TitleKind.FILM, 5)).thenAnswer(ignored -> Stream.of(title));
        when(descriptionProvider.findDescriptionBy(title)).thenReturn(Optional.of(descriptionOf(title)));

        var response = get("/resources/suggestions/5");

        assertThat(response.body()).contains("\"title\":\"Air\"", "\"myRating\":7");
    }

    @Test
    void shouldMarkSuggestionWatchedOnceIRateIt() throws Exception {
        var title = Title.builder().title("Oceniony").originalTitle("Rated").year(2020).build();
        when(titleProvider.streamOfTitles(TitleKind.FILM, 6)).thenAnswer(ignored -> Stream.of(title));
        when(descriptionProvider.findDescriptionBy(title)).thenReturn(Optional.of(descriptionOf(title)));

        var rated = put(RATINGS, """
                {"title": "Oceniony", "originalTitle": "Rated", "year": 2020, "kind": "FILM", "rating": 8,
                 "scores": [{"grade": 0.75, "quantity": 5000, "source": "Filmweb", "type": "AMATEUR",
                             "url": "https://www.filmweb.pl/film/Rated-2020-1", "significant": true},
                            {"grade": 0.8, "quantity": 9000, "source": "IMDb", "type": "AMATEUR"}]}
                """);
        var suggested = get("/resources/suggestions/6").body();
        var ratings = get(RATINGS).body();

        assertThat(rated.statusCode()).isEqualTo(204);
        assertThat(suggested).contains("\"localTitle\":\"Oceniony\"", "\"myRating\":8");
        assertThat(ratings).startsWith("title;originalTitle;year;rating;").endsWith("\nOceniony;Rated;2020;8;7.5;5000;;;FILM\n");
    }

    @Test
    void shouldRejectRatingOutsideScale() throws Exception {
        var response = put(RATINGS, """
                {"title": "Zbyt dobry", "year": 2020, "rating": 11}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(get(RATINGS).body()).doesNotContain("Zbyt dobry");
    }

    @Test
    void shouldRateTitleKnownOnlyByOriginalTitle() throws Exception {
        var response = put(RATINGS, """
                {"title": null, "originalTitle": "Only Original", "year": 2021, "rating": 7,
                 "scores": [{"grade": 0.7, "quantity": 3000, "source": "Filmweb", "type": "AMATEUR"}]}
                """);

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(get(RATINGS).body()).contains("\n;Only Original;2021;7;7.0;3000;;;FILM\n");
    }

    @Test
    void shouldRejectRatingWithoutFilmwebScore() throws Exception {
        var response = put(RATINGS, """
                {"title": "Bez Filmwebu", "year": 2023, "rating": 7,
                 "scores": [{"grade": 0.73, "quantity": 59, "source": "Metacritic", "type": "CRITIC"}]}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(get(RATINGS).body()).doesNotContain("Bez Filmwebu");
    }

    @Test
    void shouldRejectRatingWithoutYear() throws Exception {
        var response = put(RATINGS, """
                {"title": "Bez roku", "rating": 5}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(get(RATINGS).body()).doesNotContain("Bez roku");
    }

    @Test
    void shouldDownloadRatingsAsFile() throws Exception {
        var response = get(RATINGS);

        assertThat(response.headers().firstValue("Content-Disposition")).hasValue("attachment; filename=\"rated-films.csv\"");
    }

    @Test
    void shouldRemoveRating() throws Exception {
        put(RATINGS, """
                {"title": "Pomyłka", "year": 2020, "kind": "SERIES", "rating": 2,
                 "scores": [{"grade": 0.6, "quantity": 2000, "source": "Filmweb", "type": "AMATEUR"}]}
                """);

        var removed = delete(RATINGS + "?title=Pomy%C5%82ka&year=2020&kind=SERIES");
        var removedAgain = delete(RATINGS + "?title=Pomy%C5%82ka&year=2020&kind=SERIES");

        assertThat(removed.statusCode()).isEqualTo(204);
        assertThat(removedAgain.statusCode()).isEqualTo(404);
        assertThat(get(RATINGS).body()).doesNotContain("Pomyłka");
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
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        assertThat(response.body()).contains("\"detail\":\"Internal error\"").doesNotContain("internal.example");
    }

    @Test
    void shouldRejectUnknownKindWithoutNamingInternalTypes() throws Exception {
        var listed = get("/resources/suggestions/8?kind=film");
        var rated = put(RATINGS, """
                {"title": "Rodzaj", "year": 2020, "kind": "film", "rating": 7}
                """);

        assertThat(listed.statusCode()).isEqualTo(400);
        assertThat(rated.statusCode()).isEqualTo(400);
        assertThat(listed.body() + rated.body()).contains("\"status\":400").doesNotContain("pl.ciruk");
    }

    @Test
    void shouldGiveScoresOfSitesThatAnswered() throws Exception {
        when(metacriticScores.findScoresBy(any())).thenAnswer(ignored -> Stream.of(Score.critic(0.81, 44)));
        when(filmwebScores.findScoresBy(any())).thenThrow(new IllegalStateException("Throttled"));

        var response = get("/resources/scores?title=Moana&year=2016");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"grade\":0.81");
    }

    @Test
    void shouldShowMetricsOnManagementPort() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + managementPort + "/actuator/metrics")).build();

        var metrics = send(request).body();

        assertThat(metrics).contains("\"Suggestions.response\"", "\"cache.gets\"", "\"RedisCache.long.miss.count\"");
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path)).build());
    }

    private HttpResponse<String> put(String path, String json) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    private HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path)).DELETE().build());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Title title(String text) {
        return Title.builder().title(text).originalTitle(text).year(2020).build();
    }

    private static Description descriptionOf(Title title) {
        return Description.builder().title(title).foundFor(title).build();
    }
}
