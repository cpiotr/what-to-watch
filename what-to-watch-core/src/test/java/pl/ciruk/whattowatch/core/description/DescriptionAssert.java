package pl.ciruk.whattowatch.core.description;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.Optional;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class DescriptionAssert extends AbstractAssert<DescriptionAssert, Description> {
    private DescriptionAssert(Description description, Class<?> selfType) {
        super(description, selfType);
    }

    public static DescriptionAssert assertThat(Description description) {
        return new DescriptionAssert(description, DescriptionAssert.class);
    }

    public static DescriptionAssert assertThat(Optional<Description> description) {
        return new DescriptionAssert(description.orElse(Description.empty()), DescriptionAssert.class);
    }

    public DescriptionAssert hasTitle(String title) {
        var itemTitle = actual.getTitle();

        var descriptionHasTitle = itemTitle.originalTitle().equalsIgnoreCase(title)
                || itemTitle.localTitle().equalsIgnoreCase(title);

        Assertions.assertThat(descriptionHasTitle)
                .as("Expected title: <%s>. Actual: <%s>", title, itemTitle.asText())
                .isTrue();

        return this;
    }

    public DescriptionAssert hasGenre(String genre) {
        var genres = actual.getGenres();

        Assertions.assertThat(genres)
                .as("Missing genre")
                .contains(genre);

        return this;
    }

    public DescriptionAssert isEmpty() {
        Assertions.assertThat(actual.isEmpty())
                .as("Expected description to be empty. Actual: <%s>", actual)
                .isTrue();
        return this;
    }

    public DescriptionAssert hasPlot() {
        Assertions.assertThat(actual.getPlot())
                .as("Plot")
                .isNotBlank();
        return this;
    }
}
