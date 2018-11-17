package pl.ciruk.whattowatch.core.suggest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.DataModel;
import pl.ciruk.whattowatch.core.filter.FilmByScoreFilter;
import pl.ciruk.whattowatch.core.score.Score;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FilmByScoreFilterTest {

    @Test
    void shouldAcceptScoreAboveThreshold() {
        FilmByScoreFilter filter = new FilmByScoreFilter(0.35);

        List<Score> scores = List.of(
                Score.amateur(0.4, 1000),
                Score.critic(0.4, 20));
        boolean accepted = filter.test(Film.builder().scores(scores).build());

        assertThat(accepted).isTrue();
    }

    @Test
    void shouldLogScoreWhenMissedBySmallMargin() {
        Logger logger = (Logger) LoggerFactory.getLogger(FilmByScoreFilter.class);
        Appender<ILoggingEvent> appender = mock(Appender.class);
        logger.addAppender(appender);

        FilmByScoreFilter filter = new FilmByScoreFilter(0.4);

        List<Score> scores = List.of(
                Score.amateur(0.4, 1000),
                Score.critic(0.4, 20));
        Film film = Film.builder()
                .scores(scores)
                .description(DataModel.description())
                .build();

        boolean accepted = filter.test(film);

        assertThat(accepted).isFalse();

        ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
        verify(appender).doAppend(captor.capture());
        assertThat(captor.getValue().getFormattedMessage())
                .startsWith("Omitting Test title with score");
    }

    @Test
    void shouldRejectScoreWhenBelowThreshold() {
        FilmByScoreFilter filter = new FilmByScoreFilter(0.4);

        List<Score> scores = List.of(
                Score.amateur(0.3, 1000),
                Score.critic(0.3, 20));
        Film film = Film.builder()
                .scores(scores)
                .description(DataModel.description())
                .build();

        boolean accepted = filter.test(film);

        assertThat(accepted).isFalse();
    }
}
