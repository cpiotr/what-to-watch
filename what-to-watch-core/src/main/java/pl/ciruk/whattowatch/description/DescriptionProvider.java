package pl.ciruk.whattowatch.description;

import pl.ciruk.whattowatch.title.Title;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DescriptionProvider {
	Optional<Description> descriptionOf(Title title);

	default CompletableFuture<Optional<Description>> descriptionOfAsync(Title title) {
		return CompletableFuture.supplyAsync(
				() -> descriptionOf(title)
		);
	}
}