package pl.ciruk.whattowatch.suggest;

import java.util.stream.Stream;

public interface FilmSuggestionProvider {
	Stream<Film> suggestNumberOfFilms(int numberOfFilms);
}
