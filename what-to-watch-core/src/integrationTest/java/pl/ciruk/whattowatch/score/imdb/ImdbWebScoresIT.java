package pl.ciruk.whattowatch.score.imdb;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;

import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static pl.ciruk.whattowatch.score.ScoreMatchers.isMeaningful;

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

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
    }

    private Title titleOfOldAndRespectfulFilm() {
        return Title.builder()
                .title("Rambo")
                .originalTitle("First blood")
                .year(1982)
                .build();
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfRecentFilm() {
        Title title = titleOfRecentAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
    }

    @Test
    public void shouldCheckOnlyFirstTitleFromSearchResults() {
        Title title = Title.builder().originalTitle("Hidden").year(2015).build();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
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

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
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

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
    }

    private Title titleOfRecentAndRespectfulFilm() {
        return Title.builder()
                .title("Vaiana")
                .originalTitle("Moana")
                .year(2016)
                .build();
    }
}