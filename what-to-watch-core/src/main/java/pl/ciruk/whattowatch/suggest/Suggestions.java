package pl.ciruk.whattowatch.suggest;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.squareup.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.core.net.AllCookies;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.description.DescriptionProvider;
import pl.ciruk.whattowatch.description.filmweb.FilmwebDescriptions;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;
import pl.ciruk.whattowatch.title.zalukaj.ZalukajTitles;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

@Named
@Slf4j
public class Suggestions implements FilmSuggestionProvider {
	TitleProvider titles;

	DescriptionProvider descriptions;

	List<ScoresProvider> scoresProviders;

	@Inject
	public Suggestions(TitleProvider titles, DescriptionProvider descriptions, List<ScoresProvider> scoresProviders) {
		this.titles = titles;
		this.descriptions = descriptions;
		this.scoresProviders = scoresProviders;
	}

	@Override
	public CompletableFuture<List<Film>> suggestFilms() {
		log.info("suggestFilms");

		return titles.streamOfTitles()
				.map(this::getPage)
				.reduce(
						completedFuture(Stream.<Film>empty()),
						(cf1, cf2) -> cf1.thenCombine(cf2, Stream::concat)
				)
				.thenApply(stream -> stream.collect(toList()));
	}

	CompletableFuture<Stream<Film>> getPage(CompletableFuture<Stream<Title>> titlesFromPage) {
		return titlesFromPage.thenCompose(titleStream -> {
					return CompletableFutures.getAllOf(titleStream.map(this::forTitle).collect(toList()));
				}
		);
	}

	CompletableFuture<Film> forTitle(Title title) {
		CompletableFuture<Optional<Description>> descriptionOfAsync = descriptions.descriptionOfAsync(title);
		return descriptionOfAsync.thenCompose(
				optionalDescription -> optionalDescription.map(this::descriptionToFilm).orElse(completedFuture(Film.empty())));
	}

	private CompletableFuture<Film> descriptionToFilm(Description description) {
		Function<ScoresProvider, CompletableFuture<Stream<Score>>> toScoresOfAsync =
				scoresProvider -> scoresProvider.scoresOfAsync(description);

		CompletableFuture<Film> future = scoresProviders.stream()
				.map(toScoresOfAsync)
				.reduce(completedFuture(Stream.empty()), (cf1, cf2) -> cf1.thenCombine(cf2, Stream::concat))
				.thenApply(stream -> stream.collect(toList()))
				.thenApply(list -> Film.builder().description(description).scores(list).build());
		return future;
	}

	public static void main(String[] args) {
		JsoupCachedConnection connection = new JsoupCachedConnection(CacheProvider.empty(), new OkHttpClient());

		OkHttpClient httpClient = new OkHttpClient();
		new AllCookies().applyTo(httpClient);
		JsoupCachedConnection keepCookiesConnection = new JsoupCachedConnection(CacheProvider.empty(), httpClient);

		Properties properties = new Properties();
		try {
			properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application-dev.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		ZalukajTitles titles = new ZalukajTitles(
				keepCookiesConnection,
				properties.getProperty("zalukaj-login"),
				properties.getProperty("zalukaj-password"));

		Suggestions suggestions = new Suggestions(
				titles,
				new FilmwebDescriptions(connection),
				Lists.newArrayList()
		);

		Stopwatch started = Stopwatch.createStarted();
		try {
			List<Film> films = suggestions.suggestFilms().get();
			started.stop();

			System.out.println("Found films: " + films.size() + " in " + started.elapsed(TimeUnit.MILLISECONDS) + "ms");
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

	}
}
