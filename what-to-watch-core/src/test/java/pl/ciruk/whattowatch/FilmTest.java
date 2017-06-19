package pl.ciruk.whattowatch;

import org.junit.Test;
import pl.ciruk.whattowatch.score.Score;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FilmTest {
    @Test
    public void shouldBeWorthWatchingIfReceivedLotsOfPositiveScores() throws Exception {
        int totalQuantity = 1000;
        List<Score> scores = Arrays.asList(
                Score.amateur(0.7, (long) (totalQuantity * 0.95)),
                Score.amateur(0.0, (long) (totalQuantity * 0.025)),
                Score.amateur(0.0, (long) (totalQuantity * 0.025))
        );

        Film film = Film.builder()
                .scores(scores)
                .build();

        assertThat(film.isWorthWatching()).isTrue();
    }

    @Test
    public void shouldNotBeWorthWatchingIfReceivedFewPositiveScores() throws Exception {
        int totalQuantity = 100;
        List<Score> scores = Arrays.asList(
                Score.amateur(0.7, (long) (totalQuantity * 0.95)),
                Score.amateur(0.0, (long) (totalQuantity * 0.025)),
                Score.amateur(0.0, (long) (totalQuantity * 0.025))
        );

        Film film = Film.builder()
                .scores(scores)
                .build();

        assertThat(film.isWorthWatching()).isFalse();
    }
}