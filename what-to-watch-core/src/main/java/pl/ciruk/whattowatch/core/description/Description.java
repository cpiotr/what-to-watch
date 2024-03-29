package pl.ciruk.whattowatch.core.description;

import pl.ciruk.whattowatch.core.title.Title;

import java.util.List;
import java.util.Objects;

public final class Description {
    private static final Description EMPTY = Description.builder().build();

    private final Title title;
    private final String poster;
    private final String plot;
    private final List<String> genres;
    private Title foundFor;

    private Description(Title title, Title foundFor, String poster, String plot, List<String> genres) {
        this.title = title;
        this.foundFor = foundFor;
        this.poster = poster;
        this.plot = plot;
        this.genres = genres;
    }

    public static DescriptionBuilder builder() {
        return new DescriptionBuilder();
    }

    public String titleAsText() {
        return title.asText();
    }

    public int getYear() {
        return title.year();
    }

    public void foundFor(Title title) {
        foundFor = title;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public static Description empty() {
        return Description.EMPTY;
    }

    @Override
    public String toString() {
        return title.toString();
    }

    public Title getTitle() {
        return this.title;
    }

    public Title getFoundFor() {
        return this.foundFor;
    }

    public String getPoster() {
        return this.poster;
    }

    public String getPlot() {
        return this.plot;
    }

    public List<String> getGenres() {
        return this.genres;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Description other)) {
            return false;
        }
        return Objects.equals(title, other.getTitle());
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = result * 59 + (title == null ? 43 : title.hashCode());
        return result;
    }

    public static class DescriptionBuilder {
        private Title title;
        private Title foundFor;
        private String poster;
        private String plot;
        private List<String> genres;

        public Description.DescriptionBuilder title(Title title) {
            this.title = title;
            return this;
        }

        public Description.DescriptionBuilder foundFor(Title foundFor) {
            this.foundFor = foundFor;
            return this;
        }

        public Description.DescriptionBuilder poster(String poster) {
            this.poster = poster;
            return this;
        }

        public Description.DescriptionBuilder plot(String plot) {
            this.plot = plot;
            return this;
        }

        public Description.DescriptionBuilder genres(List<String> genres) {
            this.genres = genres;
            return this;
        }

        public Description build() {
            return new Description(title, foundFor, poster, plot, genres);
        }

        @Override
        public String toString() {
            return "Description.DescriptionBuilder(" +
                    "title=" + this.title + ", " +
                    "foundFor=" + this.foundFor + ", " +
                    "poster=" + this.poster + ", " +
                    "plot=" + this.plot + ", " +
                    "genres=" + this.genres +
                    ")";
        }
    }
}
