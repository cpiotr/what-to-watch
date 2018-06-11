package pl.ciruk.whattowatch.core.score.filmweb;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreAssert;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class FilmwebScoresIT {

    private FilmwebScores scores;

    @BeforeEach
    public void setUp() {
        JsoupConnection connection = TestConnections.jsoup();

        scores = new FilmwebScores(
                new FilmwebProxy(connection),
                Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldRetrieveMeaningfulScore() {
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

    private Title titleOfRecentAndRespectfulFilm() {
        return Title.builder()
                .originalTitle("La La Land")
                .year(2016)
                .build();
    }

    private Title titleOfOldAndRespectfulFilm() {
        return Title.builder()
                .title("Rambo: Pierwsza krew")
                .originalTitle("First Blood")
                .year(1982)
                .build();
    }
}