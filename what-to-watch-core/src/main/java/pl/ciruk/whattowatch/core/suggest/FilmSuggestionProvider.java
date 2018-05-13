package pl.ciruk.whattowatch.core.suggest;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface FilmSuggestionProvider {
    Stream<CompletableFuture<Film>> suggestFilms(int pageNumber);

}
