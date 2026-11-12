package pl.ciruk.whattowatch.neural

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import pl.ciruk.whattowatch.core.title.Title
import pl.ciruk.whattowatch.core.title.TitleKind

class WatchedFilmsTest {
    private val watched = WatchedFilms.of(RatedFilm.readAll())

    @Test
    fun shouldKnowRatedFilmByOriginalTitle() {
        val title = Title.builder().originalTitle("Killers of the Flower Moon").year(2023).kind(TitleKind.FILM).build()

        assertThat(watched.contains(title)).isTrue()
    }

    @Test
    fun shouldKnowRatedFilmByLocalTitleAYearApart() {
        val title = Title.builder().title("Czas krwawego księżyca").year(2024).kind(TitleKind.FILM).build()

        assertThat(watched.contains(title)).isTrue()
    }

    @Test
    fun shouldNotKnowFilmWithSameTitleFromAnotherTime() {
        val title = Title.builder().title("Air").year(1990).kind(TitleKind.FILM).build()

        assertThat(watched.contains(title)).isFalse()
    }

    @Test
    fun shouldNotTakeSeriesForFilmWithSameTitle() {
        val title = Title.builder().title("Air").year(2023).kind(TitleKind.SERIES).build()

        assertThat(watched.contains(title)).isFalse()
    }
}
