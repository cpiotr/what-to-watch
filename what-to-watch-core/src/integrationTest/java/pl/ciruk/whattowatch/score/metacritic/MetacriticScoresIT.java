package pl.ciruk.whattowatch.score.metacritic;

import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.HtmlConnection;
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
import static pl.ciruk.whattowatch.score.ScoreMatchers.isMeaningful;

public class MetacriticScoresIT {
    private JsoupConnection connection;
    private MetacriticScores scores;

    @Before
    public void setUp() throws Exception {
        connection = new JsoupConnection(new HtmlConnection(new OkHttpClient()));

        connection.init();
        scores = new MetacriticScores(connection, Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldRetrieveMeaningfulScore() throws Exception {
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
    public void shouldRetrieveMultipleScores() throws Exception {
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
}