package pl.ciruk.films.whattowatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.description.DescriptionProvider;
import pl.ciruk.films.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.films.whattowatch.score.IMDBScores;
import pl.ciruk.films.whattowatch.score.MetacriticScores;
import pl.ciruk.films.whattowatch.score.ScoresProvider;
import pl.ciruk.films.whattowatch.title.Title;
import pl.ciruk.films.whattowatch.title.TitleProvider;
import pl.ciruk.films.whattowatch.title.ekino.EkinoTitles;

public class WhatToWatch {
	TitleProvider titles;
	
	DescriptionProvider descriptions;
	
	List<ScoresProvider> scores;
	
	public WhatToWatch() {
		titles = new EkinoTitles(new JsoupConnection());
		
		descriptions = new FilmwebDescriptions(new JsoupConnection());
		
		scores = new ArrayList<ScoresProvider>();
		scores.add(new IMDBScores());
		scores.add(new MetacriticScores());
	}
	
	public void get() {
		FilmSuggestionProvider provider = i -> {
			Stream<Title> streamOfTitles = titles.streamOfTitles(i);
			
			return streamOfTitles.map(title -> descriptions.descriptionOf(title))
					.filter(Optional::isPresent)
					.map(optional -> optional.get())
					.map(description -> {
						Film film = new Film(description);
						scores.stream()
								.flatMap(scoreProvider -> scoreProvider.scoresOf(description))
								.forEach(score -> film.add(score));
						return film;
					}
			);
		};
		
		provider.suggestNumberOfFilms(12).forEach(System.out::println);
	}
	
	public static void main(String[] args) {
		WhatToWatch w2w = new WhatToWatch();
		w2w.get();
	}
}
