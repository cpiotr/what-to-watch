package pl.ciruk.whattowatch.neural

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import pl.ciruk.whattowatch.core.description.Description
import pl.ciruk.whattowatch.core.score.Score
import pl.ciruk.whattowatch.core.score.ScoreType
import pl.ciruk.whattowatch.core.score.ScoresProvider
import pl.ciruk.whattowatch.core.suggest.ScoredTitle
import pl.ciruk.whattowatch.core.title.Title
import pl.ciruk.whattowatch.core.title.TitleKind
import java.util.stream.Stream

class RatedTitleTest {
    @Test
    fun shouldReadRatingsWithScores() {
        val air = RatedTitle.readBundled().first { it.scored.description().titleAsText() == "Air" }

        assertThat(air.rating).isEqualTo(7)
        assertThat(air.scored.description().getYear()).isEqualTo(2023)
        val (filmweb, metacritic) = air.scored.scores()
        assertThat(filmweb.source()).isEqualTo("Filmweb")
        assertThat(filmweb.grade()).isCloseTo(0.701, within(1e-9))
        assertThat(filmweb.quantity()).isEqualTo(30318)
        assertThat(metacritic.source()).isEqualTo("Metacritic")
        assertThat(metacritic.grade()).isCloseTo(0.73, within(1e-9))
        assertThat(metacritic.quantity()).isEqualTo(59)
    }

    @Test
    fun shouldReadFilmsWithoutMetacriticScore() {
        val ratedTitles = RatedTitle.readBundled()

        assertThat(ratedTitles).hasSizeGreaterThan(800)
        assertThat(ratedTitles).anyMatch { rated -> rated.scored.scores().none { it.source() == "Metacritic" } }
        assertThat(ratedTitles).allMatch { rated -> rated.scored.scores().any { it.source() == "Filmweb" } }
    }

    @Test
    fun shouldAddScoresFromProvider() {
        val imdb = Score.amateur(0.75, 1000)
        val provider = ScoresProvider { Stream.of(imdb) }
        val rated = RatedTitle.readBundled().first()

        val withImdb = rated.withScoresFrom(provider)

        assertThat(withImdb.scored.scores()).containsAll(rated.scored.scores()).contains(imdb)
        assertThat(withImdb.rating).isEqualTo(rated.rating)
    }

    // Writing the file again changes no line, so a copy put back into the repository shows only new ratings
    @Test
    fun shouldWriteBundledRatingsAsTheyWereRead() {
        val bundled = javaClass.getResource("/rated-films.csv")!!.readText()

        assertThat(RatedTitle.toCsv(RatedTitle.parseAll(bundled))).isEqualTo(bundled)
    }

    @Test
    fun shouldWriteSeriesWithFilmwebAndMetacriticScoresOnly() {
        val title = Title.builder().title("Rodzina;Soprano").originalTitle("The Sopranos").year(1999).kind(TitleKind.SERIES).build()
        val scores = listOf(
                Score.builder().grade(0.87).quantity(120_000).source("Filmweb").type(ScoreType.AMATEUR).build(),
                Score.builder().grade(0.92).quantity(150_000).source("IMDb").type(ScoreType.AMATEUR).build(),
                Score.builder().grade(0.94).quantity(20).source("Metacritic").type(ScoreType.CRITIC).build())
        val sopranos = RatedTitle(ScoredTitle(Description.builder().title(title).build(), scores), 9)

        val csv = RatedTitle.toCsv(listOf(sopranos))

        assertThat(csv.lines()[1]).isEqualTo("Rodzina Soprano;The Sopranos;1999;9;8.7;120000;94;20;SERIES")
        assertThat(RatedTitle.parseAll(csv).single().title.kind()).isEqualTo(TitleKind.SERIES)
    }

    @Test
    fun shouldWriteEmptyColumnsForMissingScores() {
        val title = Title.builder().title("Nowy").year(2026).build()
        val unscored = RatedTitle(ScoredTitle(Description.builder().title(title).build(), listOf()), 4)

        assertThat(RatedTitle.toCsv(listOf(unscored)).lines()[1]).isEqualTo("Nowy;;2026;4;;;;;FILM")
    }
}
