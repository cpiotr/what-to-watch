package pl.ciruk.whattowatch.core.filter;

import pl.ciruk.whattowatch.core.suggest.Film;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FilmFilter {
    private final List<Predicate<Film>> filters;

    public FilmFilter(List<Predicate<Film>> filters) {
        this.filters = filters;
    }

    public boolean isWorthWatching(Film film) {
        if (film.isEmpty()) {
            return false;
        }

        for (Predicate<Film> filter : filters) {
            if (!filter.test(film)) {
                return false;
            }
        }

        return true;
    }
}
