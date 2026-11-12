package pl.ciruk.whattowatch.core.score.imdb;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.ScoreAssert;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.Resources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Reads the datasets that `./gradlew downloadImdbDatasets` puts in the repository's imdb directory
class ImdbDatasetScoresProviderIntegrationTest {
    private static ImdbDatasetScoresProvider scores;

    @BeforeAll
    static void loadDatasets() {
        var directory = Path.of(System.getProperty("imdb.datasets-directory", "imdb"));
        assumeTrue(
                Files.exists(directory.resolve("title.ratings.tsv.gz")) && Files.exists(directory.resolve("title.basics.tsv.gz")),
                "No IMDb datasets in " + directory.toAbsolutePath());

        scores = new ImdbDatasetScoresProvider(directory);
    }

    @Test
    void shouldRetrieveMeaningfulScores() {
        var titles = List.of(
                title("Rambo", "First blood", 1982),
                title("Vaiana", "Moana", 2016),
                title(null, "Hidden", 2015),
                title(null, "Working man", 2019),
                title(null, "Radin!", 2016),
                title("Powidoki", "Powidoki", 2016),
                title(null, "Life, animated", 2016),
                title(null, "Justice League Dark: Apokolips War", 2020),
                title(null, "The Boy, the Mole, the Fox and the Horse", 2022));

        assertThat(titles).allSatisfy(title -> assertThat(scores.findScoresBy(descriptionOf(title)))
                .as("Scores of %s", title)
                .anyMatch(ScoreAssert::isMeaningful));
    }

    @Test
    void shouldFindNearlyAllFilmsIRated() {
        var titles = Resources.readContentOf("films-with-my-scores.csv")
                .lines()
                .map(line -> line.split(";"))
                .map(columns -> title(columns[0], columns[1], Integer.parseInt(columns[2])))
                .toList();

        var found = titles.stream()
                .filter(title -> scores.findScoresBy(descriptionOf(title)).findAny().isPresent())
                .count();

        // 635 of 643 in November 2026; the misses are titles that IMDb lists only under another name
        assertThat((double) found / titles.size()).isGreaterThan(0.98);
    }

    private static Title title(String localTitle, String originalTitle, int year) {
        return Title.builder()
                .title(localTitle)
                .originalTitle(originalTitle)
                .year(year)
                .build();
    }

    private static Description descriptionOf(Title title) {
        return Description.builder()
                .title(title)
                .build();
    }
}
