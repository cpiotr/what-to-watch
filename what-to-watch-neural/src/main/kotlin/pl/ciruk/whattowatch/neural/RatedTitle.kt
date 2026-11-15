package pl.ciruk.whattowatch.neural

import pl.ciruk.whattowatch.core.description.Description
import pl.ciruk.whattowatch.core.score.Score
import pl.ciruk.whattowatch.core.score.ScoreType
import pl.ciruk.whattowatch.core.score.ScoresProvider
import pl.ciruk.whattowatch.core.suggest.ScoredTitle
import pl.ciruk.whattowatch.core.title.Title
import pl.ciruk.whattowatch.core.title.TitleKind
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A film or series I rated from 1 to 10, with the scores it has on other sites.
 */
data class RatedTitle(val scored: ScoredTitle, val rating: Int) {

    val title: Title
        get() = scored.description().title

    /**
     * Adds the scores [provider] finds for this title, such as IMDb's, which the training data can't include.
     */
    fun withScoresFrom(provider: ScoresProvider): RatedTitle {
        val scores = scored.scores() + provider.findScoresBy(scored.description()).toList()
        return copy(scored = ScoredTitle(scored.description(), scores))
    }

    /**
     * Whether this is a rating of [other], matched by title and year like the score providers do, and never a
     * series to a film with the same name.
     */
    fun isOf(other: Title): Boolean {
        return title.kind() == other.kind() && title.matches(other)
    }

    // Only Filmweb's and Metacritic's scores are kept: IMDb's terms don't allow publishing its ratings
    private fun toRow(): String {
        val filmweb = score(FILMWEB)
        val metacritic = score(METACRITIC)
        return listOf(
                text(title.localTitle()),
                text(title.originalTitle()),
                title.year(),
                rating,
                filmweb?.let { decimal(it.grade() * 10) },
                filmweb?.quantity(),
                metacritic?.let { (it.grade() * 100).roundToInt() },
                metacritic?.quantity(),
                title.kind())
                .joinToString(";") { it?.toString() ?: "" }
    }

    // A site can also score a lesser-known title of the same name, as Filmweb did for The Father (2020), so the
    // score with most votes is taken as the title's own
    private fun score(source: String): Score? {
        return scored.scores()
                .filter { it.source().equals(source, ignoreCase = true) }
                .maxByOrNull { it.quantity() }
    }

    companion object {
        private const val RESOURCE = "rated-films.csv"
        private const val HEADER = "title;originalTitle;year;rating;filmweb;filmwebVotes;metacritic;metacriticReviews;kind"
        private const val FILMWEB = "Filmweb"
        private const val METACRITIC = "Metacritic"

        /**
         * Reads the ratings bundled with the app, with their Filmweb and Metacritic scores. IMDb's terms don't allow
         * publishing its ratings, so they come from [withScoresFrom].
         */
        @JvmStatic
        fun readBundled(): List<RatedTitle> {
            val resource = checkNotNull(RatedTitle::class.java.getResource("/$RESOURCE")) { "Missing training data: $RESOURCE" }
            return parseAll(resource.readText())
        }

        fun parseAll(csv: String): List<RatedTitle> {
            return csv.lines()
                    .drop(1) // Header
                    .filter { it.isNotBlank() }
                    .map { parse(it.split(";")) }
        }

        fun toCsv(ratedTitles: List<RatedTitle>): String {
            return (listOf(HEADER) + ratedTitles.map { it.toRow() }).joinToString("\n", postfix = "\n")
        }

        // title;originalTitle;year;rating;filmweb;filmwebVotes;metacritic;metacriticReviews;kind, where rows from
        // before series could be rated have no kind
        private fun parse(columns: List<String>): RatedTitle {
            val title = Title.builder()
                    .title(columns[0].ifEmpty { null })
                    .originalTitle(columns[1].ifEmpty { null })
                    .year(columns[2].toInt())
                    .kind(columns.getOrNull(8)?.let(TitleKind::valueOf) ?: TitleKind.FILM)
                    .build()
            val scores = listOfNotNull(
                    score(columns[4], columns[5], 10.0, FILMWEB, ScoreType.AMATEUR),
                    score(columns[6], columns[7], 100.0, METACRITIC, ScoreType.CRITIC))
            return RatedTitle(ScoredTitle(Description.builder().title(title).build(), scores), columns[3].toInt())
        }

        private fun score(grade: String, quantity: String, scale: Double, source: String, type: ScoreType): Score? {
            if (grade.isEmpty()) {
                return null
            }
            return Score.builder()
                    .grade(grade.toDouble() / scale)
                    .quantity(quantity.toLong())
                    .source(source)
                    .type(type)
                    .build()
        }

        // Filmweb shows grades like 6.8 and 7.01, so that's how the file keeps them
        private fun decimal(value: Double): String = "%.2f".format(Locale.ROOT, value).removeSuffix("0")

        // The file has no quoting, so a separator or line break in a title would split the row
        private fun text(value: String?): String = value.orEmpty().replace(Regex("[;\\r\\n]"), " ")
    }
}
