package pl.ciruk.whattowatch.suggest;

import pl.ciruk.whattowatch.Film;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FilmSuggestionProvider {
	CompletableFuture<List<Film>> suggestFilms();

}
