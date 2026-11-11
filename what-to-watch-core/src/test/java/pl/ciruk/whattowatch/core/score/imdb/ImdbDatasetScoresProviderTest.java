package pl.ciruk.whattowatch.core.score.imdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.title.Title;

import java.nio.file.Path;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static pl.ciruk.whattowatch.core.score.imdb.ImdbFixtures.gzipped;

class ImdbDatasetScoresProviderTest {
    private ImdbDatasetScoresProvider provider;

    @BeforeEach
    void setUp(@TempDir Path directory) {
        gzipped("title.ratings.tsv", directory);
        gzipped("title.basics.tsv", directory);
        provider = new ImdbDatasetScoresProvider(directory, Executors.newSingleThreadExecutor());
    }

    @Test
    void shouldGiveAmateurScoreWithLinkToTitle() {
        var scores = provider.findScoresBy(description("Rambo", "First blood", 1982)).toList();

        assertThat(scores).singleElement().satisfies(score -> {
            assertThat(score.grade()).isCloseTo(0.77, within(0.0001));
            assertThat(score.quantity()).isEqualTo(308_060);
            assertThat(score.type()).isEqualTo(ScoreType.AMATEUR);
            assertThat(score.source()).isEqualTo("IMDb");
            assertThat(score.url()).isEqualTo("https://www.imdb.com/title/tt0083944");
        });
    }

    @Test
    void shouldGiveNoScoreForUnknownFilm() {
        var scores = provider.findScoresBy(description("Nieznany", "Unknown", 2020));

        assertThat(scores).isEmpty();
    }

    @Test
    void shouldGiveScoreAsynchronously() {
        var scores = provider.findScoresByAsync(description("Kraina lodu", "Frozen", 2013)).join();

        assertThat(scores).extracting(Score::url).containsExactly("https://www.imdb.com/title/tt2294629");
    }

    private static Description description(String localTitle, String originalTitle, int year) {
        return Description.builder()
                .title(Title.builder().title(localTitle).originalTitle(originalTitle).year(year).build())
                .build();
    }
}
