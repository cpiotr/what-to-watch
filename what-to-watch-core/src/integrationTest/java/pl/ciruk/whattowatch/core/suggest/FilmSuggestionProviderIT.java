package pl.ciruk.whattowatch.core.suggest;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptionProvider;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.score.filmweb.FilmwebScoresProvider;
import pl.ciruk.whattowatch.core.score.imdb.ImdbScoresProvider;
import pl.ciruk.whattowatch.core.score.metacritic.MetacriticScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.Resources;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.utils.net.HtmlConnection;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class FilmSuggestionProviderIT {
    private static final int NUMBER_OF_TITLES = 25;

    private FilmSuggestionProvider suggestions;
    private ExecutorService pool;

    @BeforeEach
    void setUp() {
        HtmlConnection htmlConnection = TestConnections.html();
        pool = Executors.newWorkStealingPool(32);
        suggestions = new FilmSuggestionProvider(
                provideTitlesFromResource(),
                sampleDescriptionProvider(htmlConnection, pool),
                sampleScoreProviders(htmlConnection, pool),
                pool,
                Caffeine.newBuilder().build()
        );
    }

    @Test
    void shouldSuggestAllFilmsFromSampleTitleProvider() {
        List<String> expectedTitles = provideTitlesFromResource().streamOfTitles(1)
                .map(Title::asText)
                .map(String::toLowerCase)
                .collect(toList());

        List<String> titles = CompletableFutures.getAllOf(suggestions.suggestFilms(1))
                .filter(Objects::nonNull)
                .filter(Film::isNotEmpty)
                .map(Film::description)
                .map(Description::getTitle)
                .map(Title::asText)
                .map(String::toLowerCase)
                .collect(toList());

        assertThat(titles).containsOnlyElementsOf(expectedTitles);
    }

    @AfterEach
    void cleanUp() throws InterruptedException {
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    private FilmwebDescriptionProvider sampleDescriptionProvider(HtmlConnection htmlConnection, ExecutorService pool) {
        FilmwebProxy filmwebProxy = new FilmwebProxy(new JsoupConnection(htmlConnection));
        return new FilmwebDescriptionProvider(filmwebProxy, pool);
    }

    private static List<ScoresProvider> sampleScoreProviders(HtmlConnection connection, ExecutorService executorService) {
        JsoupConnection jsoupConnection = new JsoupConnection(connection);
        return List.of(
                new FilmwebScoresProvider(new FilmwebProxy(jsoupConnection), executorService),
                new MetacriticScoresProvider(jsoupConnection, executorService),
                new ImdbScoresProvider(jsoupConnection, executorService)
        );
    }

    private static TitleProvider provideTitlesFromResource() {
        List<Title> titles = Resources.readContentOf("films-with-my-scores.csv")
                .lines()
                .limit(NUMBER_OF_TITLES)
                .map(line -> line.split(";"))
                .map(FilmSuggestionProviderIT::buildTitle)
                .collect(toList());
        return (int pageNumber) -> titles.stream();
    }

    private static Title buildTitle(String[] array) {
        return Title.builder()
                .title(array[0])
                .originalTitle(array[1])
                .year(Integer.parseInt(array[2]))
                .build();
    }
}