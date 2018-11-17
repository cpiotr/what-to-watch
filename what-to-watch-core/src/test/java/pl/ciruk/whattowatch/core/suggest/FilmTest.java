package pl.ciruk.whattowatch.core.suggest;

import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.score.Score;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilmTest {
    @Test
    void shouldBeWorthWatchingIfReceivedLotsOfPositiveScores() {
        long totalQuantity = 2000;
        var scores = List.of(
                Score.amateur(0.7, (long) (totalQuantity * 0.95)),
                Score.amateur(0.0, (long) (totalQuantity * 0.025)),
                Score.amateur(0.0, (long) (totalQuantity * 0.025)),
                Score.critic(0.7, 10L)
        );

        var film = Film.builder()
                .scores(scores)
                .build();

        assertThat(film.normalizedScore())
                .isGreaterThan(0.65);
    }

    @Test
    void shouldNotBeWorthWatchingIfReceivedFewPositiveScores() {
        int totalQuantity = 100;
        var scores = List.of(
                Score.amateur(0.7, (long) (totalQuantity * 0.4)),
                Score.amateur(0.7, (long) (totalQuantity * 0.55)),
                Score.amateur(0.0, (long) (totalQuantity * 0.025)),
                Score.amateur(0.0, (long) (totalQuantity * 0.025)),
                Score.critic(0.7, 9L)
        );

        var film = Film.builder()
                .scores(scores)
                .build();

        assertThat(film.normalizedScore())
                .isLessThan(0.65);
    }
}
