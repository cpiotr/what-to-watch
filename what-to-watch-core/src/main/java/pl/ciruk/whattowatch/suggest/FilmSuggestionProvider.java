package pl.ciruk.whattowatch.suggest;

import pl.ciruk.whattowatch.Film;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface FilmSuggestionProvider {
    Stream<CompletableFuture<Film>> suggestFilms();

}
