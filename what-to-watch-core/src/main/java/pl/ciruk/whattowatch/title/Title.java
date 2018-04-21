package pl.ciruk.whattowatch.title;

import java.util.Optional;

import static pl.ciruk.core.stream.Predicates.not;

public class Title {
    public static final int MISSING_YEAR = 0;

    private String originalTitle;

    private String title;

    private int year;

    private String url;

    private Title(String originalTitle, String title, int year, String url) {
        this.originalTitle = originalTitle;
        this.title = title;
        this.year = year;
        this.url = url;
    }

    public static TitleBuilder builder() {
        return new TitleBuilder();
    }

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

    public String getOriginalTitle() {
        return this.originalTitle;
    }

    public String getTitle() {
        return this.title;
    }

    public int getYear() {
        return this.year;
    }

    public String getUrl() {
        return this.url;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Title)) return false;
        final Title other = (Title) o;
        if (!other.canEqual(this)) return false;
        final Object this$originalTitle = this.getOriginalTitle();
        final Object other$originalTitle = other.getOriginalTitle();
        if (this$originalTitle == null ? other$originalTitle != null : !this$originalTitle.equals(other$originalTitle))
            return false;
        final Object this$title = this.getTitle();
        final Object other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        return this.getYear() == other.getYear();
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $originalTitle = this.getOriginalTitle();
        result = result * PRIME + ($originalTitle == null ? 43 : $originalTitle.hashCode());
        final Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        result = result * PRIME + this.getYear();
        return result;
    }

    protected boolean canEqual(Object other) {
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

        public String toString() {
            return "Title.TitleBuilder(originalTitle=" + this.originalTitle + ", title=" + this.title + ", year=" + this.year + ", url=" + this.url + ")";
        }
    }
}
