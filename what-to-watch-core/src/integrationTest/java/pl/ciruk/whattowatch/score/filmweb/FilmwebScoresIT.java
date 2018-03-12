package pl.ciruk.whattowatch.score.filmweb;

import com.codahale.metrics.MetricRegistry;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoreMatcher;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;

public class FilmwebScoresIT {

    private FilmwebScores scores;

    @Before
    public void setUp() {
        JsoupConnection connection = TestConnections.jsoup();

        scores = new FilmwebScores(
                new FilmwebProxy(connection),
                mock(MetricRegistry.class),
                Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldRetrieveMeaningfulScore() {
        Title title = titleOfOldAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).anyMatch(ScoreMatcher::isMeaningful);
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfRecentFilm() {
        Title title = titleOfRecentAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scores = this.scores.scoresOf(description);

        Assertions.assertThat(scores).anyMatch(ScoreMatcher::isMeaningful);
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