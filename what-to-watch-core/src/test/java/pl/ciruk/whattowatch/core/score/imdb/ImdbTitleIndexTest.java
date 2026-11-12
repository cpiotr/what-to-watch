package pl.ciruk.whattowatch.core.score.imdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.ciruk.whattowatch.core.score.imdb.ImdbFixtures.gzipped;

class ImdbTitleIndexTest {
    private static final String RAMBO = "Rambo";

    private ImdbTitleIndex index;

    @BeforeEach
    void setUp(@TempDir Path directory) throws IOException {
        index = ImdbTitleIndex.load(
                gzipped("title.ratings.tsv", directory),
                gzipped("title.basics.tsv", directory),
                100);
    }

    @Test
    void shouldFindByOriginalTitle() {
        var found = index.find(title(RAMBO, "First blood", 1982));

        assertThat(found).contains(new ImdbTitleIndex.ImdbTitle("tt0083944", 1982, 7.7, 308_060, TitleKind.FILM));
    }

    @Test
    void shouldFindByLocalTitleWhenOriginalIsMissing() {
        var found = index.find(title("Radin!", null, 2016));

        assertThat(found).map(ImdbTitleIndex.ImdbTitle::id).contains("tt5253294");
    }

    @Test
    void shouldIgnorePolishDiacritics() {
        var found = index.find(title("Katyń", "Katyń", 2007));

        assertThat(found).map(ImdbTitleIndex.ImdbTitle::id).contains("tt0879843");
    }

    @Test
    void shouldTreatAmpersandAsAnd() {
        var found = index.find(title("Głupi i głupszy", "Dumb & Dumber", 1994));

        assertThat(found).map(ImdbTitleIndex.ImdbTitle::id).contains("tt0109686");
    }

    @Test
    void shouldAcceptYearOffByOne() {
        var found = index.find(title("Miś", "", 1980));

        assertThat(found).map(ImdbTitleIndex.ImdbTitle::id).contains("tt0082755");
    }

    @Test
    void shouldPreferTitleWithMostVotes() {
        var found = index.find(title("Kraina lodu", "Frozen", 2013));

        assertThat(found).map(ImdbTitleIndex.ImdbTitle::id).contains("tt2294629");
    }

    @Test
    void shouldNotMatchDistantYear() {
        assertThat(index.find(title(RAMBO, "First blood", 1985))).isEmpty();
    }

    @Test
    void shouldSkipSeriesWhenAskedForFilm() {
        assertThat(index.find(title(RAMBO, null, 2008))).isEmpty();
    }

    @Test
    void shouldFindSeriesWhenAskedForSeries() {
        var found = index.find(title(RAMBO, null, 2008, TitleKind.SERIES));

        assertThat(found).map(ImdbTitleIndex.ImdbTitle::id).contains("tt1084971");
    }

    @Test
    void shouldSkipFilmWhenAskedForSeries() {
        assertThat(index.find(title(RAMBO, "First blood", 1982, TitleKind.SERIES))).isEmpty();
    }

    @Test
    void shouldSkipTitlesWithFewVotes() {
        assertThat(index.find(title("Few votes", null, 2010))).isEmpty();
    }

    @Test
    void shouldSkipTitlesWithoutYear() {
        assertThat(index.find(title("No year", null, 2010))).isEmpty();
    }

    private static Title title(String localTitle, String originalTitle, int year) {
        return title(localTitle, originalTitle, year, TitleKind.FILM);
    }

    private static Title title(String localTitle, String originalTitle, int year, TitleKind kind) {
        return Title.builder()
                .title(localTitle)
                .originalTitle(originalTitle)
                .year(year)
                .kind(kind)
                .build();
    }
}
