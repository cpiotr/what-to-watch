package pl.ciruk.whattowatch.neural

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import pl.ciruk.whattowatch.core.description.Description
import pl.ciruk.whattowatch.core.score.Score
import pl.ciruk.whattowatch.core.score.ScoreType
import pl.ciruk.whattowatch.core.suggest.Film
import pl.ciruk.whattowatch.core.title.Title

class PersonalRatingsTest {
    private val ratedFilms = RatedFilm.readAll()
    private val personalRatings = PersonalRatings.trainOn(ratedFilms)

    @Test
    fun shouldRankFilmsItWasNotTrainedOnAtLeastAsWellAsSuggestionScore() {
        val folds = 5
        val predictions = (0 until folds).flatMap { fold ->
            val model = PersonalRatings.trainOn(ratedFilms.filterIndexed { index, _ -> index % folds != fold })
            ratedFilms.filterIndexed { index, _ -> index % folds == fold }
                    .mapNotNull { rated -> model.predict(rated.film)?.let { rated to it } }
        }
        val ratings = predictions.map { it.first.rating.toDouble() }.toDoubleArray()
        val predicted = predictions.map { it.second }.toDoubleArray()
        val suggestionScores = predictions.map { it.first.film.normalizedScore() }.toDoubleArray()

        assertThat(predictions).hasSizeGreaterThan(ratedFilms.size - 10)
        assertThat(spearman(predicted, ratings)).isGreaterThanOrEqualTo(spearman(suggestionScores, ratings))
    }

    @Test
    fun shouldPredictHigherRatingForBetterScores() {
        val good = film(filmweb = 0.8, imdb = 0.8, metacritic = 0.85)
        val poor = film(filmweb = 0.5, imdb = 0.5, metacritic = 0.4)

        assertThat(personalRatings.predict(good)).isGreaterThan(personalRatings.predict(poor))
    }

    @Test
    fun shouldPredictWithImdbAlone() {
        val rating = personalRatings.predict(film(imdb = 0.7))

        assertThat(rating).isBetween(1.0, 10.0)
    }

    @Test
    fun shouldPredictNothingWithoutAudienceScore() {
        assertThat(personalRatings.predict(film(metacritic = 0.9))).isNull()
    }

    @Test
    fun shouldIgnoreScoresWithTooFewVotes() {
        val withFewVotes = film(filmweb = 0.9, votes = 10)

        assertThat(personalRatings.predict(withFewVotes)).isNull()
    }

    private fun film(filmweb: Double? = null, imdb: Double? = null, metacritic: Double? = null, votes: Long = 10_000): Film {
        val scores = listOfNotNull(
                filmweb?.let { score(it, votes, "Filmweb", ScoreType.AMATEUR) },
                imdb?.let { score(it, votes, "IMDb", ScoreType.AMATEUR) },
                metacritic?.let { score(it, 40, "Metacritic", ScoreType.CRITIC) })
        val title = Title.builder().title("Film").year(2026).build()
        return Film(Description.builder().title(title).build(), scores)
    }

    private fun score(grade: Double, quantity: Long, source: String, type: ScoreType): Score {
        return Score.builder().grade(grade).quantity(quantity).source(source).type(type).build()
    }

    private fun spearman(first: DoubleArray, second: DoubleArray): Double {
        return pearson(ranks(first), ranks(second))
    }

    // Tied values share the mean of their ranks
    private fun ranks(values: DoubleArray): DoubleArray {
        val ranks = DoubleArray(values.size)
        values.indices.groupBy { values[it] }
                .toSortedMap()
                .values
                .fold(0) { below, tied ->
                    tied.forEach { ranks[it] = below + (tied.size + 1) / 2.0 }
                    below + tied.size
                }
        return ranks
    }

    private fun pearson(first: DoubleArray, second: DoubleArray): Double {
        val firstMean = first.average()
        val secondMean = second.average()
        val covariance = first.indices.sumOf { (first[it] - firstMean) * (second[it] - secondMean) }
        val firstSpread = first.sumOf { (it - firstMean) * (it - firstMean) }
        val secondSpread = second.sumOf { (it - secondMean) * (it - secondMean) }
        return covariance / Math.sqrt(firstSpread * secondSpread)
    }
}
