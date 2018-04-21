package pl.ciruk.whattowatch.description;

import pl.ciruk.whattowatch.title.Title;

import java.util.List;

public class Description {
    private static final Description EMPTY = Description.builder().build();

    private Title title;

    private Title foundFor;

    private String poster;

    private String plot;

    private List<String> genres;

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
        return title.getYear();
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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Description)) {
            return false;
        }
        final Description other = (Description) o;
        if (!other.canEqual(this)) {
            return false;
        }
        final Object this$title = this.getTitle();
        final Object other$title = other.getTitle();
        return this$title == null ? other$title == null : this$title.equals(other$title);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        return result;
    }

    private boolean canEqual(Object other) {
        return other instanceof Description;
    }

    public static class DescriptionBuilder {
        private Title title;
        private Title foundFor;
        private String poster;
        private String plot;
        private List<String> genres;

        DescriptionBuilder() {
        }

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

        public String toString() {
            return "Description.DescriptionBuilder(title=" + this.title + ", foundFor=" + this.foundFor + ", poster=" + this.poster + ", plot=" + this.plot + ", genres=" + this.genres + ")";
        }
    }
}
