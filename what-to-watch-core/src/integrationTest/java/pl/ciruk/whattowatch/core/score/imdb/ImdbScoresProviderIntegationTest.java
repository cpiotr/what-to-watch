package pl.ciruk.whattowatch.core.score.imdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.ScoreAssert;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class ImdbScoresProviderIntegationTest {
    private ScoresProvider scores;

    @BeforeEach
    void setUp() {
        JsoupConnection connection = TestConnections.jsoup();

        scores = new ImdbScoresProvider(connection, Executors.newSingleThreadExecutor());
    }

    @Test
    void shouldRetrieveMeaningfulScoreOfOldFilm() {
        Title title = titleOfOldAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        assertScoresForDescriptionAreMeaningful(description);
    }

    @Test
    void shouldRetrieveMeaningfulScoreOfRecentFilm() {
        Title title = titleOfRecentAndRespectfulFilm();
        Description description = Description.builder()
                .title(title)
                .build();

        assertScoresForDescriptionAreMeaningful(description);
    }

    @Test
    void shouldCheckOnlyFirstTitleFromSearchResults() {
        Title title = Title.builder().originalTitle("Hidden").year(2015).build();
        Description description = Description.builder()
                .title(title)
                .build();

        assertScoresForDescriptionAreMeaningful(description);
    }

    @Test
    void shouldFindFilmWhenTwoTitlesMatchButOneIsInsignificant() {
        Title title = Title.builder().originalTitle("Working man").year(2019).build();
        Description description = Description.builder()
                .title(title)
                .build();

        var foundScores = this.scores.findScoresBy(description).collect(toList());
        assertThat(foundScores)
                .anyMatch(ScoreAssert::isMeaningful);
    }

    @Test
    void shouldRetrieveMeaningfulScoreOfFilmWithSpecialCharsInTitle() {
        Title title = Title.builder()
                .originalTitle("Radin!")
                .year(2016)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        assertScoresForDescriptionAreMeaningful(description);
    }

    @Test
    void shouldRetrieveMeaningfulScoreOfPolishFilm() {
        Title title = Title.builder()
                .originalTitle("Powidoki")
                .title("Powidoki")
                .year(2016)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        assertScoresForDescriptionAreMeaningful(description);
    }

    @Test
    void shouldRetrieveMeaningfulScoreOfDocumentary() {
        Title title = Title.builder()
                .originalTitle("Life, animated")
                .year(2016)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        assertScoresForDescriptionAreMeaningful(description);
    }

    @Test
    void shouldRetrieveMeaningfulScoreOfAnimatedVideo() {
        Title title = Title.builder()
                .originalTitle("Justice League Dark: Apokolips War")
                .year(2020)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        assertScoresForDescriptionAreMeaningful(description);
    }

    @Test
    void shouldRetrieveMeaningfulScoreOfShortAnimatedVideo() {
        Title title = Title.builder()
                .originalTitle("The Boy, the Mole, the Fox and the Horse")
                .year(2022)
                .build();
        Description description = Description.builder()
                .title(title)
                .build();

        assertScoresForDescriptionAreMeaningful(description);
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

    private void assertScoresForDescriptionAreMeaningful(Description description) {
        var scores = this.scores.findScoresBy(description).toList();
        assertThat(scores)
                .anyMatch(ScoreAssert::isMeaningful);
    }
}
