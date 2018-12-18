package pl.ciruk.whattowatch.core.score.imdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreAssert;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ImdbScoresIT {
    private ScoresProvider scores;

    @BeforeEach
    public void setUp() {
        JsoupConnection connection = TestConnections.jsoup();

        scores = new ImdbScores(connection, Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfOldFilm() {
        Title title = titleOfOldAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfRecentFilm() {
        Title title = titleOfRecentAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
    }

    @Test
    public void shouldCheckOnlyFirstTitleFromSearchResults() {
        Title title = Title.builder().originalTitle("Hidden").year(2015).build();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
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

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
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

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfDocumentary() {
        Title title = Title.builder()
                .originalTitle("Life, animated")
                .year(2016)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.findScoresBy(description);

        assertThat(scores).anyMatch(ScoreAssert::isMeaningful);
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