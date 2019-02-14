package pl.ciruk.whattowatch.core.suggest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.score.filmweb.FilmwebScores;
import pl.ciruk.whattowatch.core.score.imdb.ImdbScores;
import pl.ciruk.whattowatch.core.score.metacritic.MetacriticScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.utils.net.HtmlConnection;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class FilmSuggestionsIT {
    private static final int NUMBER_OF_TITLES = 50;

    private FilmSuggestions suggestions;
    private ExecutorService pool;

    @BeforeEach
    public void setUp() {
        HtmlConnection htmlConnection = TestConnections.html();
        pool = Executors.newWorkStealingPool(32);
        suggestions = new FilmSuggestions(
                provideTitlesFromResource(),
                sampleDescriptionProvider(htmlConnection, pool),
                sampleScoreProviders(htmlConnection, pool),
                pool,
                new ConcurrentHashMap<>()
        );
    }

    @Test
    public void shouldSuggestAllFilmsFromSampleTitleProvider() {
        Stream<Film> films = CompletableFutures.getAllOf(
                suggestions.suggestFilms(1));
        int numberOfFilms = (int) films
                .filter(Objects::nonNull)
                .filter(Film::isNotEmpty)
                .count();

        assertThat(numberOfFilms).isEqualTo(NUMBER_OF_TITLES);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    private FilmwebDescriptions sampleDescriptionProvider(HtmlConnection htmlConnection, ExecutorService pool) {
        FilmwebProxy filmwebProxy = new FilmwebProxy(new JsoupConnection(htmlConnection));
        return new FilmwebDescriptions(filmwebProxy, pool);
    }

    private static List<ScoresProvider> sampleScoreProviders(HtmlConnection connection, ExecutorService executorService) {
        JsoupConnection jsoupConnection = new JsoupConnection(connection);
        return List.of(
                new FilmwebScores(new FilmwebProxy(jsoupConnection), executorService),
                new MetacriticScoresProvider(jsoupConnection, executorService),
                new ImdbScores(jsoupConnection, executorService)
        );
    }

    private static TitleProvider provideTitlesFromResource() {
        try (InputStream inputStream = getResourceAsStream(); BufferedReader reader = createReader(inputStream)) {
            List<Title> titles = reader.lines()
                    .limit(NUMBER_OF_TITLES)
                    .map(line -> line.split(";"))
                    .map(FilmSuggestionsIT::buildTitle)
                    .collect(toList());
            return (int pageNumber) -> titles.stream();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Title buildTitle(String[] array) {
        return Title.builder()
                .title(array[0])
                .originalTitle(array[1])
                .year(Integer.parseInt(array[2]))
                .build();
    }

    private static InputStream getResourceAsStream() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("films-with-my-scores.csv");
    }

    private static BufferedReader createReader(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
    }
}