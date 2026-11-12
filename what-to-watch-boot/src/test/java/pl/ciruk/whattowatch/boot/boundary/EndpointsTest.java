package pl.ciruk.whattowatch.boot.boundary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.filter.FilmFilter;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
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
    @MockitoBean(name = "filmFilter")
    private FilmFilter filmFilter;

    @BeforeEach
    void setUp() {
        when(filmFilter.isWorthWatching(any())).thenReturn(true);
    }

    @Test
    void shouldSearchFilmsOfPageAtOnce() throws Exception {
        var first = title("First");
        var second = title("Second");
        when(titleProvider.streamOfTitles(1)).thenAnswer(ignored -> Stream.of(first, second));
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
