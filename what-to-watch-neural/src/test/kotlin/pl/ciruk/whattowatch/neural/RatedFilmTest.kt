package pl.ciruk.whattowatch.neural

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import pl.ciruk.whattowatch.core.score.Score
import pl.ciruk.whattowatch.core.score.ScoresProvider
import java.util.stream.Stream

class RatedFilmTest {
    @Test
    fun shouldReadRatingsWithScores() {
        val air = RatedFilm.readAll().first { it.film.description().titleAsText() == "Air" }

        assertThat(air.rating).isEqualTo(7)
        assertThat(air.film.description().getYear()).isEqualTo(2023)
        val (filmweb, metacritic) = air.film.scores()
        assertThat(filmweb.source()).isEqualTo("Filmweb")
        assertThat(filmweb.grade()).isCloseTo(0.701, within(1e-9))
        assertThat(filmweb.quantity()).isEqualTo(30318)
        assertThat(metacritic.source()).isEqualTo("Metacritic")
        assertThat(metacritic.grade()).isCloseTo(0.73, within(1e-9))
        assertThat(metacritic.quantity()).isEqualTo(59)
    }

    @Test
    fun shouldReadFilmsWithoutMetacriticScore() {
        val films = RatedFilm.readAll()

        assertThat(films).hasSizeGreaterThan(800)
        assertThat(films).anyMatch { film -> film.film.scores().none { it.source() == "Metacritic" } }
        assertThat(films).allMatch { film -> film.film.scores().any { it.source() == "Filmweb" } }
    }

    @Test
    fun shouldAddScoresFromProvider() {
        val imdb = Score.amateur(0.75, 1000)
        val provider = ScoresProvider { Stream.of(imdb) }
        val film = RatedFilm.readAll().first()

        val withImdb = film.withScoresFrom(provider)

        assertThat(withImdb.film.scores()).containsAll(film.film.scores()).contains(imdb)
        assertThat(withImdb.rating).isEqualTo(film.rating)
    }
}
