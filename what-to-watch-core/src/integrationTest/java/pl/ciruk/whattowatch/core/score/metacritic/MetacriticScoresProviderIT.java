package pl.ciruk.whattowatch.core.score.metacritic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreAssert;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MetacriticScoresProviderIT {
    private MetacriticScoresProvider scores;

    @BeforeEach
    void setUp() {
        JsoupConnection connection = TestConnections.jsoup();

        scores = new MetacriticScoresProvider(connection, Executors.newSingleThreadExecutor());
    }

    @Test
    void shouldRetrieveMeaningfulScore() {
        Title title = titleOfRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).allMatch(ScoreAssert::isMeaningful);
    }

    @Test
    void shouldRetrieveMeaningfulScoreWhenTitleCausesRedirects() {
        Title title = titleOfFilmCausingRedirects();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).allMatch(ScoreAssert::isMeaningful);
    }

    @Test
    void shouldRetrieveMeaningfulScoreForTitleWithSpecialChars() {
        Title title = Title.builder()
                .title("T2: Trainspotting")
                .year(2017)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).allMatch(ScoreAssert::isMeaningful);
    }

    @Test
    void shouldRetrieveMeaningfulScoreForTitleWithSlash() {
        Title title = Title.builder()
                .title("Fahrenheit 9/11")
                .year(2004)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).allMatch(ScoreAssert::isMeaningful);
    }

    @Test
    void shouldRetrieveMultipleScores() {
        Title title = titleOfRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        long numberOfScores = scores.findScoresBy(description)
                .count();

        assertThat(numberOfScores).isGreaterThan(1L);
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