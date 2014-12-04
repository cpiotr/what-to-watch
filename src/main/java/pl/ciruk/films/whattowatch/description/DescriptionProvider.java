package pl.ciruk.films.whattowatch.description;

import java.util.Optional;

import pl.ciruk.films.whattowatch.title.Title;

public interface DescriptionProvider {
	Optional<Description> descriptionOf(Title title);
}
