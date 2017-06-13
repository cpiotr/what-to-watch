package pl.ciruk.whattowatch.score.filmweb;

import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;

import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.Assert.assertThat;
import static pl.ciruk.whattowatch.score.ScoreMatchers.isMeaningful;

public class FilmwebScoresIT {

    private JsoupConnection connection;
    private FilmwebScores scores;

    @Before
    public void setUp() throws Exception {
        connection = new JsoupConnection(new HtmlConnection(OkHttpClient::new));

        connection.init();
        scores = new FilmwebScores(new FilmwebProxy(connection), Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldRetrieveMeaningfulScore() throws Exception {
        Title title = titleOfOldAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
    }

    @Test
    public void shouldRetrieveMeaningfulScoreOfRecentFilm() throws Exception {
        Title title = titleOfRecentAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        Stream<Score> scoreStream = scores.scoresOf(description);
        Score score = scoreStream
                .findAny()
                .orElseThrow(AssertionError::new);

        assertThat(score, isMeaningful());
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