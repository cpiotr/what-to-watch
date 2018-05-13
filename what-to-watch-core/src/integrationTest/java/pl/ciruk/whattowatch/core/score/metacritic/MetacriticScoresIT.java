package pl.ciruk.whattowatch.core.score.metacritic;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreAssert;
import pl.ciruk.whattowatch.core.title.Title;

import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class MetacriticScoresIT {
    private MetacriticScores scores;

    @Before
    public void setUp() {
        JsoupConnection connection = TestConnections.jsoup();

        scores = new MetacriticScores(connection, Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldRetrieveMeaningfulScore() {
        Title title = titleOfRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).allMatch(ScoreAssert::isMeaningful);
    }

    @Test
    public void shouldRetrieveMeaningfulScoreWhenTitleCausesRedirects() {
        Title title = titleOfFilmCausingRedirects();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).allMatch(ScoreAssert::isMeaningful);
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

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).allMatch(ScoreAssert::isMeaningful);
    }

    @Test
    public void shouldRetrieveMultipleScores() {
        Title title = titleOfRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        long numberOfScores = scores.scoresOf(description)
                .count();

        Assertions.assertThat(numberOfScores).isGreaterThan(1L);
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