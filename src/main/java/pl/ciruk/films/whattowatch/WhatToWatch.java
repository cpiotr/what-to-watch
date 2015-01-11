package pl.ciruk.films.whattowatch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import pl.ciruk.core.net.JsoupConnection;
import pl.ciruk.films.whattowatch.description.DescriptionProvider;
import pl.ciruk.films.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.films.whattowatch.score.filmweb.FilmwebScores;
import pl.ciruk.films.whattowatch.score.imdb.IMDBScores;
import pl.ciruk.films.whattowatch.score.metacritic.MetacriticScores;
import pl.ciruk.films.whattowatch.score.ScoresProvider;
import pl.ciruk.films.whattowatch.title.Title;
import pl.ciruk.films.whattowatch.title.TitleProvider;
import pl.ciruk.films.whattowatch.title.ekino.EkinoTitles;

public class WhatToWatch {
	TitleProvider titles;
	
	DescriptionProvider descriptions;
	
	List<ScoresProvider> scoresProviders;
	
	public WhatToWatch() {
		titles = new EkinoTitles(new JsoupConnection());
		titles = i -> {
			Splitter splitter = Splitter.on(';');
			try {
				return Files.readAllLines(
						Paths.get(
								Thread.currentThread()
										.getContextClassLoader()
										.getResource("film-scores.csv")
										.toURI()))
						.stream()
						.map(line -> {
							List<String> scoreElemets = splitter.splitToList(line);
							return new Title(
									scoreElemets.get(0),
									scoreElemets.get(1),
									Integer.valueOf(scoreElemets.get(2)));
						});
			} catch (IOException | URISyntaxException e) {
				throw new RuntimeException(e);
			}
		};
		
		descriptions = new FilmwebDescriptions(new JsoupConnection());

		scoresProviders = new ArrayList<>();
		scoresProviders.add(new IMDBScores());
		scoresProviders.add(new MetacriticScores(new JsoupConnection()));
		scoresProviders.add(new FilmwebScores());
	}
	
	public void get() {
		FilmSuggestionProvider provider = i -> {
			Stream<Title> streamOfTitles = titles.streamOfTitles(i);
			
			return streamOfTitles
					.parallel()
					.map(title -> descriptions.descriptionOf(title))
					.filter(Optional::isPresent)
					.map(optional -> optional.get())
					.map(description -> {
						Film film = new Film(description);
						scoresProviders.stream()
								.parallel()
								.flatMap(scoreProvider -> scoreProvider.scoresOf(description))
								.forEach(score -> film.add(score));
						return film;
					}
			);
		};
		
		provider.suggestNumberOfFilms(12).forEach((Film x) -> {
			System.out.println(x + " " +  x.scores);
		});
	}
	
	public static void main(String[] args) {
		WhatToWatch w2w = new WhatToWatch();
		w2w.get();
	}
}
