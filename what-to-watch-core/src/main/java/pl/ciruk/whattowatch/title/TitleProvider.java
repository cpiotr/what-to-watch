package pl.ciruk.whattowatch.title;

import java.util.stream.Stream;

public interface TitleProvider {
	Stream<Title> streamOfTitles();
}
