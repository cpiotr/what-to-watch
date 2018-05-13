package pl.ciruk.whattowatch.core.title;

import java.util.stream.Stream;

public interface TitleProvider {
    Stream<Title> streamOfTitles(int pageNumber);
}
