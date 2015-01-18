package pl.ciruk.films.whattowatch;

import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.description.DescriptionProvider;
import pl.ciruk.films.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.films.whattowatch.score.Score;
import pl.ciruk.films.whattowatch.score.ScoresProvider;
import pl.ciruk.films.whattowatch.score.filmweb.FilmwebScores;
import pl.ciruk.films.whattowatch.score.imdb.IMDBScores;
import pl.ciruk.films.whattowatch.score.metacritic.MetacriticScores;
import pl.ciruk.films.whattowatch.title.Title;
import pl.ciruk.films.whattowatch.title.TitleProvider;
import pl.ciruk.films.whattowatch.title.ekino.EkinoTitles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static pl.ciruk.core.math.WilsonScore.confidenceIntervalLowerBound;

public class WhatToWatch {
	TitleProvider titles;
	
	DescriptionProvider descriptions;
	
	List<ScoresProvider> scoresProviders;
	
	public WhatToWatch() {
		titles = new EkinoTitles(new JsoupConnection());
		
		descriptions = new FilmwebDescriptions(new JsoupConnection());

		scoresProviders = new ArrayList<>();
		scoresProviders.add(new IMDBScores());
		scoresProviders.add(new MetacriticScores(new JsoupConnection()));
		scoresProviders.add(new FilmwebScores());
	}
	
	public List<Film> get(int numberOfFilms) {
		FilmSuggestionProvider provider = i -> {
			Stream<Title> streamOfTitles = titles.streamOfTitles()
					.limit(i);
			
			return streamOfTitles
					.parallel()
					.map(descriptions::descriptionOf)
					.filter(Optional::isPresent)
					.map(optional -> optional.get())
					.map(description -> {
						Film film = new Film(description);
						scoresProviders.stream()
								.parallel()
								.flatMap(scoreProvider -> scoreProvider.scoresOf(description))
								.forEach(score -> film.add(score));
						return film;
					})
					.filter(film -> film.scores.stream().mapToDouble(Score::getScore).average().orElse(0.0) > 0.6)
					.filter(film -> film.numberOfScores() > 1)
					.sorted((first, second) -> {
						return second.normalizedScore().compareTo(first.normalizedScore());
					});
		};

		return provider.suggestNumberOfFilms(numberOfFilms)
//				.forEach((Film x) -> {
//					System.out.println(titles.urlFor(x.foundFor()) + " " + x + " " + x.scores + " " + x.poster());
//				})
				.peek(f -> f.setLink(
						titles.urlFor(f.foundFor())))
				.collect(toList());
	}
	
	public static void main(String[] args) {
		WhatToWatch w2w = new WhatToWatch();
		w2w.get(50);
	}
}
