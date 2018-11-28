package pl.ciruk.whattowatch;

import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.title.Title;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class DataModel {
    private DataModel() {
        throw new AssertionError();
    }

    public static Description description() {
        return Description.builder()
                .title(title())
                .build();
    }

    public static Title title() {
        return Title.builder()
                .title("Test title")
                .build();
    }
}
