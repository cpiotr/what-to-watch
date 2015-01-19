package pl.ciruk.films.whattowatch;

import java.util.stream.Stream;

public interface FilmSuggestionProvider {
	Stream<Film> suggestNumberOfFilms(int numberOfFilms); 
}
