package pl.ciruk.whattowatch.neural

import pl.ciruk.whattowatch.core.score.Score
import pl.ciruk.whattowatch.core.suggest.ScoredTitle

/**
 * Predicts how I'd rate a film, from 1 to 10, with weights for its Filmweb, IMDb and Metacritic scores
 * learned from films I rated.
 *
 * Vote counts and the release year would rank my rated films slightly better, but every rated film has years of
 * votes behind it. New releases have few, and the model would read that as a sign I'd like them.
 */
class PersonalRatings private constructor(
        private val model: RidgeRegression,
        private val meanMetacriticGrade: Double) {

    /**
     * Returns null for a film with neither a Filmweb nor an IMDb score.
     */
    fun predict(film: ScoredTitle): Double? {
        return features(film, meanMetacriticGrade)
                ?.let { model.predict(it).coerceIn(MIN_RATING, MAX_RATING) }
    }

    companion object {
        private const val MIN_RATING = 1.0
        private const val MAX_RATING = 10.0
        private const val FILMWEB = "Filmweb"
        private const val IMDB = "IMDb"
        private const val METACRITIC = "Metacritic"

        // Barely shrinks the weights at hundreds of films, and keeps them stable when a source is
        // missing for every film, as it is without IMDb's datasets
        private const val PENALTY = 10.0

        @JvmStatic
        fun trainOn(ratedFilms: List<RatedFilm>): PersonalRatings {
            val meanMetacriticGrade = ratedFilms.mapNotNull { grade(it.film, METACRITIC) }.average()
                    .takeUnless { it.isNaN() } ?: 0.0
            val usable = ratedFilms.mapNotNull { rated -> features(rated.film, meanMetacriticGrade)?.let { it to rated.rating } }
            val model = RidgeRegression.fit(
                    usable.map { it.first },
                    usable.map { it.second.toDouble() }.toDoubleArray(),
                    PENALTY)
            return PersonalRatings(model, meanMetacriticGrade)
        }

        // Filmweb and IMDb agree closely, so each stands in for the other when one is missing
        private fun features(film: ScoredTitle, meanMetacriticGrade: Double): DoubleArray? {
            val filmweb = grade(film, FILMWEB)
            val imdb = grade(film, IMDB)
            val metacritic = grade(film, METACRITIC)
            val audience = filmweb ?: imdb ?: return null
            return doubleArrayOf(
                    audience,
                    imdb ?: audience,
                    if (imdb == null) 1.0 else 0.0,
                    metacritic ?: meanMetacriticGrade,
                    if (metacritic == null) 1.0 else 0.0)
        }

        // Same rule as the suggestions' own score: too few votes or reviews don't count
        private fun grade(film: ScoredTitle, source: String): Double? {
            return film.scores()
                    .firstOrNull { it.isSignificant && it.source().equals(source, ignoreCase = true) }
                    ?.let(Score::grade)
        }
    }
}
