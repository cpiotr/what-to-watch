package pl.ciruk.whattowatch.title;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

import static pl.ciruk.core.stream.Predicates.not;

@Getter
@Builder
@EqualsAndHashCode(exclude = {"url"})
public class Title {
    public static final int MISSING_YEAR = 0;

    private String originalTitle;

    private String title;

    private int year;

    private String url;

    public String asText() {
        return Optional.ofNullable(getOriginalTitle())
                .filter(not(String::isEmpty))
                .orElse(getTitle());
    }

    public boolean matches(Title otherTitle) {
        boolean hasMatchingTitle = matchesAlphanumerically(otherTitle.getOriginalTitle(), title)
                || matchesAlphanumerically(otherTitle.getTitle(), title)
                || matchesAlphanumerically(otherTitle.getOriginalTitle(), originalTitle)
                || matchesAlphanumerically(otherTitle.getTitle(), originalTitle);
        boolean hasMatchingYear = Math.abs(otherTitle.year - year) <= 1;

        return hasMatchingTitle && hasMatchingYear;
    }

    private boolean matchesAlphanumerically(String first, String second) {
        if (first == null || second == null) {
            return false;
        } else {
            return unify(first).equals(unify(second));
        }
    }

    private String unify(String text) {
        return text
                .replaceAll("\\W", "")
                .toLowerCase();
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", asText(), year);
    }
}
