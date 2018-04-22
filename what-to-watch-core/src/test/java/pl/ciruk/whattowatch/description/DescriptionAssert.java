package pl.ciruk.whattowatch.description;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import pl.ciruk.whattowatch.title.Title;

public class DescriptionAssert extends AbstractAssert<DescriptionAssert, Description> {

    private DescriptionAssert(Description description, Class<?> selfType) {
        super(description, selfType);
    }

    public static DescriptionAssert assertThat(Description description) {
        return new DescriptionAssert(description, DescriptionAssert.class);
    }

    public DescriptionAssert hasTitle(String title) {
        var itemTitle = actual.getTitle();

        var descriptionHasTitle = itemTitle.getOriginalTitle().equalsIgnoreCase(title)
                || itemTitle.getTitle().equalsIgnoreCase(title);

        Assertions.assertThat(descriptionHasTitle)
                .as("Has title: " + title)
                .isTrue();

        return this;
    }
}
