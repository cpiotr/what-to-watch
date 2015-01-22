package pl.ciruk.films.whattowatch.suggest;

import pl.ciruk.films.whattowatch.Film;

import java.util.stream.Stream;

public interface FilmSuggestionProvider {
	Stream<Film> suggestNumberOfFilms(int numberOfFilms);
}
