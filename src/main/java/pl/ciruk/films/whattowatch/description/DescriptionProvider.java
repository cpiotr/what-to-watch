package pl.ciruk.films.whattowatch.description;

import pl.ciruk.films.whattowatch.title.Title;

public interface DescriptionProvider {
	Description descriptionOf(Title title);
}
