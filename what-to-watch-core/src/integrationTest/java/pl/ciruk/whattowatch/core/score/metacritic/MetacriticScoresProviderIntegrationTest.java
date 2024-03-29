package pl.ciruk.whattowatch.core.score.metacritic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreAssert;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MetacriticScoresProviderIntegrationTest {
    private MetacriticScoresProvider scoresProvider;

    @BeforeEach
    void setUp() {
        JsoupConnection connection = TestConnections.jsoup();

        scoresProvider = new MetacriticScoresProvider(connection, Executors.newSingleThreadExecutor());
    }

    @Test
    void shouldRetrieveMeaningfulScore() {
        Title title = titleOfRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        try (Stream<Score> scores = scoresProvider.findScoresBy(description)) {
            assertThat(scores)
                    .isNotEmpty()
                    .allMatch(ScoreAssert::isMeaningful);
        }
    }

    @Test
    void shouldRetrieveMeaningfulScoreWhenTitleCausesRedirects() {
        Title title = titleOfFilmCausingRedirects();
        Description description = Description.builder()
                .title(title)
                .build();

        try (Stream<Score> scores = scoresProvider.findScoresBy(description)) {
            assertThat(scores)
                    .isNotEmpty()
                    .allMatch(ScoreAssert::isMeaningful);
        }
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

        try (Stream<Score> scores = scoresProvider.findScoresBy(description)) {
            assertThat(scores).allMatch(ScoreAssert::isMeaningful);
        }
    }

    @Test
    void shouldRetrieveMeaningfulScoreForTitleWhenCriticScoresRedirectToSearch() {
        Title title = Title.builder()
                .title("Fantastic Beasts: The Secrets of Dumbledore")
                .year(2022)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        try (Stream<Score> scores = scoresProvider.findScoresBy(description)) {
            assertThat(scores)
                    .isNotEmpty()
                    .allMatch(ScoreAssert::isMeaningful);
        }
    }

    @Test
    void shouldRetrieveMeaningfulScoreForTitleWhichRepeatsTwoYearsInARow() {
        Title title = Title.builder()
                .title("Little women")
                .year(2019)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        List<Score> scores = scoresProvider.findScoresBy(description).toList();
        assertThat(scores).isNotEmpty();
        scores.forEach(score -> ScoreAssert.assertThat(score).hasGradeGreaterThan(0.8));
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

        try (Stream<Score> scores = scoresProvider.findScoresBy(description)) {
            assertThat(scores)
                    .isNotEmpty()
                    .allMatch(ScoreAssert::isMeaningful);
        }
    }

    @Test
    void shouldRetrieveMultipleScores() {
        Title title = titleOfRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        long numberOfScores = scoresProvider.findScoresBy(description)
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
