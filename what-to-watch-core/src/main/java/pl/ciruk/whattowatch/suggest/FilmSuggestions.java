package pl.ciruk.whattowatch.suggest;

import lombok.extern.slf4j.Slf4j;
import pl.ciruk.whattowatch.Film;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.description.DescriptionProvider;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static pl.ciruk.core.concurrent.CompletableFutures.combineUsing;

@Named
@Slf4j
public class FilmSuggestions implements FilmSuggestionProvider {
	TitleProvider titles;

	DescriptionProvider descriptions;

	List<ScoresProvider> scoresProviders;

	final ExecutorService executorService;

	@Inject
	public FilmSuggestions(
			TitleProvider titles,
			DescriptionProvider descriptions,
			List<ScoresProvider> scoresProviders,
			ExecutorService executorService) {
		this.titles = titles;
		this.descriptions = descriptions;
		this.scoresProviders = scoresProviders;
		this.executorService = executorService;
	}

	@Override
	public Stream<CompletableFuture<Film>> suggestFilms() {
		log.info("suggestFilms");

		return titles.streamOfTitles()
				.map(this::forTitle);
	}

	CompletableFuture<Film> forTitle(Title title) {
		CompletableFuture<Optional<Description>> descriptionOfAsync = descriptions.descriptionOfAsync(title);
		return descriptionOfAsync.thenComposeAsync(
				optionalDescription -> optionalDescription
						.map(this::descriptionToFilm)
						.orElse(completedFuture(Film.empty())))
				.exceptionally(t -> {
					log.error("Cannot get film for title {}", title, t);
					return Film.empty();
				});
	}

	private CompletableFuture<Film> descriptionToFilm(Description description) {
		Function<ScoresProvider, CompletableFuture<Stream<Score>>> toScoresOfAsync =
				scoresProvider -> scoresProvider.scoresOfAsync(description);

		return scoresProviders.stream()
				.map(toScoresOfAsync)
				.reduce(completedFuture(Stream.empty()), combineUsing(Stream::concat, executorService))
				.thenApply(stream -> stream.collect(toList()))
				.thenApply(list -> Film.builder()
						.description(description)
						.scores(list)
						.build());
	}
}
