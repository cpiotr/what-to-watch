package pl.ciruk.whattowatch.core.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.suggest.Film;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilmFilterTest {
    private FilmFilter filmFilter;
    private Predicate<Film> firstFilter;
    private Predicate<Film> secondFilter;

    @BeforeEach
    void setUp() {
        firstFilter = mockPredicate();
        secondFilter = mockPredicate();
        filmFilter = new FilmFilter(List.of(firstFilter, secondFilter));
    }

    @Test
    void shouldAcceptFilmWhenAllFiltersPassed() {
        when(firstFilter.test(any())).thenReturn(true);
        when(secondFilter.test(any())).thenReturn(true);

        boolean worthWatching = filmFilter.isWorthWatching(Film.builder().build());

        assertThat(worthWatching).isTrue();
    }

    @Test
    void shouldRejectFilmWhenAnyFilterFailed() {
        when(firstFilter.test(any())).thenReturn(true);
        when(secondFilter.test(any())).thenReturn(false);

        boolean worthWatching = filmFilter.isWorthWatching(Film.builder().build());

        assertThat(worthWatching).isFalse();
    }

    @Test
    void shouldRejectEmptyFilm() {
        when(firstFilter.test(any())).thenReturn(true);
        when(secondFilter.test(any())).thenReturn(true);

        boolean worthWatching = filmFilter.isWorthWatching(Film.empty());

        assertThat(worthWatching).isFalse();
    }


    @SuppressWarnings("unchecked")
    private Predicate<Film> mockPredicate() {
        return mock(Predicate.class);
    }
}
