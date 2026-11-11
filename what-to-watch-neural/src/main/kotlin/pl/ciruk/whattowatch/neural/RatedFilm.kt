package pl.ciruk.whattowatch.neural

import pl.ciruk.whattowatch.core.description.Description
import pl.ciruk.whattowatch.core.score.Score
import pl.ciruk.whattowatch.core.score.ScoreType
import pl.ciruk.whattowatch.core.score.ScoresProvider
import pl.ciruk.whattowatch.core.suggest.Film
import pl.ciruk.whattowatch.core.title.Title

/**
 * A film I rated from 1 to 10, with the scores it has on other sites.
 */
data class RatedFilm(val film: Film, val rating: Int) {

    /**
     * Adds the scores [provider] finds for this film, such as IMDb's, which the training data can't include.
     */
    fun withScoresFrom(provider: ScoresProvider): RatedFilm {
        val scores = film.scores() + provider.findScoresBy(film.description()).toList()
        return copy(film = Film(film.description(), scores))
    }

    companion object {
        private const val RESOURCE = "rated-films.csv"

        /**
         * Reads my ratings with their Filmweb and Metacritic scores. IMDb's terms don't allow publishing its ratings,
         * so they come from [withScoresFrom].
         */
        @JvmStatic
        fun readAll(): List<RatedFilm> {
            val resource = checkNotNull(RatedFilm::class.java.getResource("/$RESOURCE")) { "Missing training data: $RESOURCE" }
            return resource.readText()
                    .lines()
                    .drop(1) // Header
                    .filter { it.isNotBlank() }
                    .map { parse(it.split(";")) }
        }

        // title;originalTitle;year;rating;filmweb;filmwebVotes;metacritic;metacriticReviews
        private fun parse(columns: List<String>): RatedFilm {
            val title = Title.builder()
                    .title(columns[0])
                    .originalTitle(columns[1].ifEmpty { null })
                    .year(columns[2].toInt())
                    .build()
            val scores = listOfNotNull(
                    score(columns[4], columns[5], 10.0, "Filmweb", ScoreType.AMATEUR),
                    score(columns[6], columns[7], 100.0, "Metacritic", ScoreType.CRITIC))
            return RatedFilm(Film(Description.builder().title(title).build(), scores), columns[3].toInt())
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
    }
}
