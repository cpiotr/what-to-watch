package pl.ciruk.whattowatch.core.description;

import pl.ciruk.whattowatch.core.title.Title;

import java.util.Optional;

public interface DescriptionProvider {
    Optional<Description> findDescriptionBy(Title title);
}
