package pl.ciruk.films.whattowatch;

import pl.ciruk.films.whattowatch.suggest.FilmSuggestionProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Named
public class WhatToWatch {

	private FilmSuggestionProvider suggestions;

	@Inject
	public WhatToWatch(FilmSuggestionProvider suggestions) {
		this.suggestions = suggestions;
	}

	public List<Film> get(int numberOfFilms) {
		return suggestions.suggestNumberOfFilms(numberOfFilms)
				.collect(toList());
	}
}
