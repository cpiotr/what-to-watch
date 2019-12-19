package pl.ciruk.whattowatch.core.title;

import pl.ciruk.whattowatch.utils.text.Text;

import java.util.Objects;
import java.util.Optional;

import static pl.ciruk.whattowatch.utils.stream.Predicates.not;

public class Title {
    public static final int MISSING_YEAR = 0;

    private final String originalTitle;
    private final String localTitle;
    private final int year;
    private final String url;

    private Title(String originalTitle, String localTitle, int year, String url) {
        this.originalTitle = originalTitle;
        this.localTitle = localTitle;
        this.year = year;
        this.url = url;
    }

    public static TitleBuilder builder() {
        return new TitleBuilder();
    }

    public String asText() {
        return Optional.ofNullable(getOriginalTitle())
                .filter(not(String::isEmpty))
                .orElse(getLocalTitle())
                .replace("/", " ");
    }

    public boolean matches(Title otherTitle) {
        boolean hasMatchingTitle = Text.matchesAlphanumerically(otherTitle.getOriginalTitle(), localTitle)
                || Text.matchesAlphanumerically(otherTitle.getLocalTitle(), localTitle)
                || Text.matchesAlphanumerically(otherTitle.getOriginalTitle(), originalTitle)
                || Text.matchesAlphanumerically(otherTitle.getLocalTitle(), originalTitle);
        boolean hasMatchingYear = Math.abs(otherTitle.year - year) <= 1;

        return hasMatchingTitle && hasMatchingYear;
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", asText(), year);
    }

    public String getOriginalTitle() {
        return this.originalTitle;
    }

    public String getLocalTitle() {
        return this.localTitle;
    }

    public int getYear() {
        return this.year;
    }

    public String getUrl() {
        return this.url;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Title)) {
            return false;
        }
        final Title other = (Title) object;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!Objects.equals(this.getOriginalTitle(), other.getOriginalTitle())) {
            return false;
        }
        if (!Objects.equals(this.getLocalTitle(), other.getLocalTitle())) {
            return false;
        }
        return this.getYear() == other.getYear();
    }

    @Override
    public int hashCode() {
        int prime = 59;
        int result = 1;
        final Object originalTitle = this.getOriginalTitle();
        result = result * prime + (originalTitle == null ? 43 : originalTitle.hashCode());
        final Object title = this.getLocalTitle();
        result = result * prime + (title == null ? 43 : title.hashCode());
        result = result * prime + this.getYear();
        return result;
    }

    private boolean canEqual(Object other) {
        return other instanceof Title;
    }

    public static class TitleBuilder {
        private String originalTitle;
        private String title;
        private int year;
        private String url;

        TitleBuilder() {
        }

        public Title.TitleBuilder originalTitle(String originalTitle) {
            this.originalTitle = originalTitle;
            return this;
        }

        public Title.TitleBuilder title(String title) {
            this.title = title;
            return this;
        }

        public Title.TitleBuilder year(int year) {
            this.year = year;
            return this;
        }

        public Title.TitleBuilder url(String url) {
            this.url = url;
            return this;
        }

        public Title build() {
            return new Title(originalTitle, title, year, url);
        }

        @Override
        public String toString() {
            return "Title.TitleBuilder(originalTitle=" + this.originalTitle + ", title=" + this.title + ", year=" + this.year + ", url=" + this.url + ")";
        }
    }
}
