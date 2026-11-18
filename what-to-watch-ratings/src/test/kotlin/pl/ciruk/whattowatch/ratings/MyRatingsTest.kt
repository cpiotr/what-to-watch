package pl.ciruk.whattowatch.ratings

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pl.ciruk.whattowatch.core.description.Description
import pl.ciruk.whattowatch.core.suggest.ScoredTitle
import pl.ciruk.whattowatch.core.title.Title
import pl.ciruk.whattowatch.core.title.TitleKind
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class MyRatingsTest {
    @TempDir
    private lateinit var directory: Path

    private val file: Path
        get() = directory.resolve("ratings/rated-films.csv")

    @Test
    fun shouldStartFromBundledRatings() {
        val ratings = open()

        assertThat(ratings.all()).isEqualTo(RatedTitle.readBundled())
        assertThat(file).hasContent(javaClass.getResource("/rated-films.csv")!!.readText())
    }

    @Test
    fun shouldKeepRatingAfterOpeningFileAgain() {
        open().rate(rated(title("Nowy film", 2026), 8))

        val reopened = open()

        assertThat(reopened.ratingOf(title("Nowy film", 2026))).isEqualTo(8)
        assertThat(Files.readAllLines(file).last()).isEqualTo("Nowy film;;2026;8;;;;;FILM")
    }

    @Test
    fun shouldChangeRatingInPlaceOfEarlierOne() {
        val ratings = open()
        val before = Files.readAllLines(file)

        ratings.rate(rated(title("Air", 2023), 9))

        val after = Files.readAllLines(file)
        assertThat(after).hasSameSizeAs(before)
        assertThat(after.filter { it !in before }).containsExactly("Air;;2023;9;;;;;FILM")
        assertThat(ratings.ratingOf(title("Air", 2023))).isEqualTo(9)
    }

    @Test
    fun shouldForgetRemovedRating() {
        open().remove(title("Air", 2023))

        val reopened = open()

        assertThat(reopened.ratingOf(title("Air", 2023))).isNull()
        assertThat(reopened.remove(title("Air", 2023))).isFalse()
    }

    @Test
    fun shouldKnowRatedFilmByOriginalTitle() {
        val title = Title.builder().originalTitle("Killers of the Flower Moon").year(2023).kind(TitleKind.FILM).build()

        assertThat(open().ratingOf(title)).isEqualTo(6)
    }

    @Test
    fun shouldKnowRatedFilmByLocalTitleAYearApart() {
        assertThat(open().ratingOf(title("Czas krwawego księżyca", 2024))).isEqualTo(6)
    }

    @Test
    fun shouldNotKnowFilmWithSameTitleFromAnotherTime() {
        assertThat(open().ratingOf(title("Air", 1990))).isNull()
    }

    @Test
    fun shouldKeepSeriesApartFromFilmWithSameTitle() {
        val ratings = open()
        val series = Title.builder().title("Air").year(2023).kind(TitleKind.SERIES).build()

        assertThat(ratings.ratingOf(series)).isNull()
        ratings.rate(rated(series, 3))
        assertThat(ratings.ratingOf(series)).isEqualTo(3)
        assertThat(ratings.ratingOf(title("Air", 2023))).isEqualTo(7)
    }

    @Test
    fun shouldLearnAgainFromChangedRatings() {
        val learnedFrom = mutableListOf<List<RatedTitle>>()
        val ratings = MyRatings.open(file) { learnedFrom.add(it); PersonalRatings.trainOn(it) }

        ratings.rate(rated(title("Nowy film", 2026), 8))
        ratings.remove(title("Nowy film", 2026))

        assertThat(learnedFrom).hasSize(3)
        assertThat(learnedFrom[1]).hasSize(learnedFrom[0].size + 1)
        assertThat(learnedFrom[2]).isEqualTo(learnedFrom[0])
    }

    @Test
    fun shouldNotShowRatingThatWasNotSaved() {
        val ratings = open()
        // A directory where the new file would be written makes saving fail
        assertThat(file.resolveSibling("rated-films.csv.tmp").toFile().mkdir()).isTrue()

        // Not assertThrows or runCatching: their lambdas make SpotBugs report casts in other tests
        val failure = try {
            ratings.rate(rated(title("Nowy film", 2026), 8))
            null
        } catch (e: IOException) {
            e
        }

        assertThat(failure).isNotNull()
        assertThat(ratings.ratingOf(title("Nowy film", 2026))).isNull()
    }

    // Rows written before series could be rated have no kind
    @Test
    fun shouldReadRowsWithoutKindAsFilms() {
        open()
        Files.writeString(file, "title;originalTitle;year;rating;filmweb;filmwebVotes;metacritic;metacriticReviews\nStary;;1999;6;7.5;5000;;\n")

        assertThat(open().ratingOf(title("Stary", 1999))).isEqualTo(6)
    }

    private fun open(): MyRatings = MyRatings.open(file) { PersonalRatings.trainOn(it) }

    private fun title(text: String, year: Int): Title {
        return Title.builder().title(text).year(year).kind(TitleKind.FILM).build()
    }

    private fun rated(title: Title, rating: Int): RatedTitle {
        return RatedTitle(ScoredTitle(Description.builder().title(title).build(), listOf()), rating)
    }
}
