package pl.ciruk.whattowatch.core.description;

import pl.ciruk.whattowatch.core.title.Title;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DescriptionProvider {
    Optional<Description> descriptionOf(Title title);

    CompletableFuture<Optional<Description>> descriptionOfAsync(Title title);
}
