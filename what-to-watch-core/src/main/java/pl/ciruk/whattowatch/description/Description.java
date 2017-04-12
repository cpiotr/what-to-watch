package pl.ciruk.whattowatch.description;

import lombok.Builder;
import lombok.Getter;
import pl.ciruk.whattowatch.title.Title;

import java.util.List;

@Getter
@Builder
public class Description {
    private static final Description EMPTY = Description.builder().build();

    private Title title;

    private Title foundFor;

    private String poster;

    private String plot;

    private List<String> genres;

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
}
