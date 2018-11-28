package pl.ciruk.whattowatch.core.description;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class DescriptionAssert extends AbstractAssert<DescriptionAssert, Description> {
    private DescriptionAssert(Description description, Class<?> selfType) {
        super(description, selfType);
    }

    public static DescriptionAssert assertThat(Description description) {
        return new DescriptionAssert(description, DescriptionAssert.class);
    }

    public DescriptionAssert hasTitle(String title) {
        var itemTitle = actual.getTitle();

        var descriptionHasTitle = itemTitle.getOriginalTitle().equalsIgnoreCase(title)
                || itemTitle.getLocalTitle().equalsIgnoreCase(title);

        Assertions.assertThat(descriptionHasTitle)
                .as("Expected title: <%s>. Actual: <%s>", title, itemTitle.asText())
                .isTrue();

        return this;
    }
}
