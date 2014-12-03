package pl.ciruk.films.whattowatch;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import pl.ciruk.films.whattowatch.description.Description;
import pl.ciruk.films.whattowatch.description.DescriptionProvider;
import pl.ciruk.films.whattowatch.score.Score;
import pl.ciruk.films.whattowatch.score.ScoresProvider;
import pl.ciruk.films.whattowatch.title.Title;
import pl.ciruk.films.whattowatch.title.TitleProvider;

public class WhatToWatch {
	SecureRandom random;
	
	{
		try {
			random = SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	TitleProvider titles;
	
	DescriptionProvider descriptions;
	
	List<ScoresProvider> scores;
	
	public WhatToWatch() {
		titles = i -> {
			return Arrays.asList("Name of the rose", "Zodiac", "Gone girl", "Argo")
					.parallelStream()
					.map(s -> new Title(s));
		};
		
		descriptions = t -> {
			return new Description(t);
		};
		
		scores = new ArrayList<ScoresProvider>();
		scores.add(d -> {
			return Stream.of(
					new Score(random.nextDouble(), random.nextInt(10_000)),
					new Score(random.nextDouble(), random.nextInt(10_000)),
					new Score(random.nextDouble(), random.nextInt(10_000))
			);
		});
		
		scores.add(d -> {
			return Stream.of(
					new Score(random.nextDouble(), random.nextInt(10_000))
			);
		});
		
		scores.add(d -> {
			return Stream.of(
					new Score(random.nextDouble(), random.nextInt(10_000))
			);
		});
	}
	
	public void get() {
		FilmSuggestionProvider provider = i -> {
			Stream<Title> streamOfTitles = titles.streamOfTitles(i);
			
			return streamOfTitles.map(title -> descriptions.descriptionOf(title))
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
