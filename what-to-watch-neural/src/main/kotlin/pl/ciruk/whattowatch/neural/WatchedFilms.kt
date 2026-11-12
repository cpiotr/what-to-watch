package pl.ciruk.whattowatch.neural

import pl.ciruk.whattowatch.core.title.Title

/**
 * Films I've already seen. For now these are the films I rated, so a film I watched without rating it isn't known.
 */
class WatchedFilms(private val titles: List<Title>) {

    /**
     * Matches by title and year like the score providers do, and never a series to a film with the same name.
     */
    fun contains(title: Title): Boolean {
        return titles.any { it.kind() == title.kind() && it.matches(title) }
    }

    companion object {
        @JvmStatic
        fun of(ratedFilms: List<RatedFilm>): WatchedFilms {
            return WatchedFilms(ratedFilms.map { it.film.description().title })
        }
    }
}
