package pl.ciruk.whattowatch.core.title;

import pl.ciruk.whattowatch.utils.text.Texts;

import java.util.Optional;

import static pl.ciruk.whattowatch.utils.stream.Predicates.not;

public record Title(
        String originalTitle,
        String localTitle,
        int year,
        String url) {
    public static final int MISSING_YEAR = 0;

    public static TitleBuilder builder() {
        return new TitleBuilder();
    }

    public String asText() {
        return Optional.ofNullable(originalTitle)
                .filter(not(String::isEmpty))
                .orElse(localTitle)
                .replace("/", " ");
    }

    public boolean matches(Title otherTitle) {
        boolean hasMatchingTitle = Texts.matchesAlphanumerically(otherTitle.originalTitle(), localTitle)
                || Texts.matchesAlphanumerically(otherTitle.localTitle(), localTitle)
                || Texts.matchesAlphanumerically(otherTitle.originalTitle(), originalTitle)
                || Texts.matchesAlphanumerically(otherTitle.localTitle(), originalTitle);
        boolean hasMatchingYear = Math.abs(otherTitle.year - year) <= 1;

        return hasMatchingTitle && hasMatchingYear;
    }

    public static class TitleBuilder {
        private String originalTitle;
        private String title;
        private int year;
        private String url;

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
