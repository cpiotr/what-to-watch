package pl.ciruk.whattowatch.core.filter;

import pl.ciruk.whattowatch.core.suggest.ScoredTitle;

import java.util.List;
import java.util.function.Predicate;

public class SuggestionFilter {
    private final List<Predicate<ScoredTitle>> filters;

    public SuggestionFilter(List<Predicate<ScoredTitle>> filters) {
        this.filters = filters;
    }

    public boolean isWorthWatching(ScoredTitle scored) {
        if (scored.isEmpty()) {
            return false;
        }

        for (Predicate<ScoredTitle> filter : filters) {
            if (!filter.test(scored)) {
                return false;
            }
        }

        return true;
    }
}
