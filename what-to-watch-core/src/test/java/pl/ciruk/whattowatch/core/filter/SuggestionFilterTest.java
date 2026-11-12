package pl.ciruk.whattowatch.core.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.suggest.ScoredTitle;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SuggestionFilterTest {
    private SuggestionFilter suggestionFilter;
    private Predicate<ScoredTitle> firstFilter;
    private Predicate<ScoredTitle> secondFilter;

    @BeforeEach
    void setUp() {
        firstFilter = mockPredicate();
        secondFilter = mockPredicate();
        suggestionFilter = new SuggestionFilter(List.of(firstFilter, secondFilter));
    }

    @Test
    void shouldAcceptFilmWhenAllFiltersPassed() {
        when(firstFilter.test(any())).thenReturn(true);
        when(secondFilter.test(any())).thenReturn(true);

        boolean worthWatching = suggestionFilter.isWorthWatching(new ScoredTitle(null, null));

        assertThat(worthWatching).isTrue();
    }

    @Test
    void shouldRejectFilmWhenAnyFilterFailed() {
        when(firstFilter.test(any())).thenReturn(true);
        when(secondFilter.test(any())).thenReturn(false);

        boolean worthWatching = suggestionFilter.isWorthWatching(new ScoredTitle(null, null));

        assertThat(worthWatching).isFalse();
    }

    @Test
    void shouldRejectEmptyFilm() {
        when(firstFilter.test(any())).thenReturn(true);
        when(secondFilter.test(any())).thenReturn(true);

        boolean worthWatching = suggestionFilter.isWorthWatching(ScoredTitle.empty());

        assertThat(worthWatching).isFalse();
    }

    @SuppressWarnings("unchecked")
    private Predicate<ScoredTitle> mockPredicate() {
        return mock(Predicate.class);
    }
}
