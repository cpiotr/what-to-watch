package pl.ciruk.whattowatch.score.imdb;

import com.codahale.metrics.MetricRegistry;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreAssert;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;

public class ImdbWebScoresIT {
    private ScoresProvider scores;

    @Before
    public void setUp() {
        JsoupConnection connection = TestConnections.jsoup();

        scores = new ImdbWebScores(connection, mock(MetricRegistry.class), Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfOldFilm() {
        Title title = titleOfOldAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfRecentFilm() {
        Title title = titleOfRecentAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
    }

    @Test
    public void shouldCheckOnlyFirstTitleFromSearchResults() {
        Title title = Title.builder().originalTitle("Hidden").year(2015).build();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfFilmWithSpecialCharsInTitle() {
        Title title = Title.builder()
                .originalTitle("Radin!")
                .year(2016)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfPolishFilm() {
        Title title = Title.builder()
                .originalTitle("Powidoki")
                .title("Powidoki")
                .year(2016)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
    }

    private Title titleOfOldAndRespectfulFilm() {
        return Title.builder()
                .title("Rambo")
                .originalTitle("First blood")
                .year(1982)
                .build();
    }

    private Title titleOfRecentAndRespectfulFilm() {
        return Title.builder()
                .title("Vaiana")
                .originalTitle("Moana")
                .year(2016)
                .build();
    }
}