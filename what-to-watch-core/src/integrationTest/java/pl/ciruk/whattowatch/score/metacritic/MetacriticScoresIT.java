package pl.ciruk.whattowatch.score.metacritic;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreMatchers;
import pl.ciruk.whattowatch.title.Title;

import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static pl.ciruk.whattowatch.score.ScoreMatchers.isMeaningful;

public class MetacriticScoresIT {
    private MetacriticScores scores;

    @Before
    public void setUp() {
        JsoupConnection connection = TestConnections.jsoup();

        scores = new MetacriticScores(connection, mock(MetricRegistry.class), Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldRetrieveMeaningfulScore() {
        Title title = titleOfRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .filter(ScoreMatchers::isMeaningful)
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
    }

    @Test
    public void shouldRetrieveMeaningfulScoreWhenTitleCausesRedirects() {
        Title title = titleOfFilmCausingRedirects();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .filter(ScoreMatchers::isMeaningful)
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
    }

    @Test
    public void shouldRetrieveMeaningfulScoreForTitleWithSpecialChars() {
        Title title = Title.builder()
                .title("T2: Trainspotting")
                .year(2017)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .filter(ScoreMatchers::isMeaningful)
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
    }

    @Test
    public void shouldRetrieveMultipleScores() {
        Title title = titleOfRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        long numberOfScores = scores.scoresOf(description)
                .count();

        assertThat(numberOfScores, is(greaterThan(1L)));
    }

    private Title titleOfRespectfulFilm() {
        return Title.builder()
                .originalTitle("Moana")
                .year(2016)
                .build();
    }

    private Title titleOfFilmCausingRedirects() {
        return Title.builder()
                .originalTitle("The post")
                .year(2017)
                .build();
    }
}