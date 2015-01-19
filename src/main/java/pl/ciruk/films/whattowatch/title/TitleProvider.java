package pl.ciruk.films.whattowatch.title;

import java.util.stream.Stream;

public interface TitleProvider {
	Stream<Title> streamOfTitles();

	String urlFor(Title title);
}
