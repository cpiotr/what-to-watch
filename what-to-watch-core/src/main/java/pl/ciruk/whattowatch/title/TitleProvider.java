package pl.ciruk.whattowatch.title;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface TitleProvider {
	Stream<CompletableFuture<Stream<Title>>> streamOfTitles();

	String urlFor(Title title);
}
