package pl.ciruk.whattowatch.description;

import pl.ciruk.whattowatch.title.Title;

import java.util.Optional;

public interface DescriptionProvider {
	Optional<Description> descriptionOf(Title title);
}
