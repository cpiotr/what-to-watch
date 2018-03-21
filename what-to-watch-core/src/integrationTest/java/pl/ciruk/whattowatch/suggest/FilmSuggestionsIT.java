package pl.ciruk.whattowatch.suggest;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.Film;
import pl.ciruk.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.score.filmweb.FilmwebScores;
import pl.ciruk.whattowatch.score.imdb.ImdbWebScores;
import pl.ciruk.whattowatch.score.metacritic.MetacriticScores;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class FilmSuggestionsIT {
    private static final int NUMBER_OF_TITLES = 50;

    private FilmSuggestions suggestions;
    private ExecutorService pool;

    @Before
    public void setUp() {
        HtmlConnection htmlConnection = TestConnections.html();
        pool = Executors.newWorkStealingPool(32);
        suggestions = new FilmSuggestions(
                provideTitlesFromResource(),
                sampleDescriptionProvider(htmlConnection, pool),
                sampleScoreProviders(htmlConnection, pool),
                pool
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

        Assertions.assertThat(numberOfFilms).isEqualTo(NUMBER_OF_TITLES);
    }

    @After
    public void cleanUp() throws InterruptedException {
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
    }

    private FilmwebDescriptions sampleDescriptionProvider(HtmlConnection htmlConnection, ExecutorService pool) {
        return new FilmwebDescriptions(
                new FilmwebProxy(new JsoupConnection(htmlConnection)),
                pool);
    }

    private static List<ScoresProvider> sampleScoreProviders(HtmlConnection connection, ExecutorService executorService) {
        JsoupConnection jsoupConnection = new JsoupConnection(connection);
        return List.of(
                new FilmwebScores(new FilmwebProxy(jsoupConnection), executorService),
                new MetacriticScores(jsoupConnection, executorService),
                new ImdbWebScores(jsoupConnection, executorService)
        );
    }

    private static TitleProvider provideTitlesFromResource() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("films-with-my-scores.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()))) {
            List<Title> titles = reader.lines()
                    .limit(NUMBER_OF_TITLES)
                    .map(line -> line.split(";"))
                    .map(array -> Title.builder().title(array[0]).originalTitle(array[1]).year(Integer.parseInt(array[2])).build())
                    .collect(toList());
            return (int pageNumber) -> titles.stream();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}