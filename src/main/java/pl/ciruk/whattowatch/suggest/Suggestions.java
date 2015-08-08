package pl.ciruk.whattowatch.suggest;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ManagedAsync;
import pl.ciruk.core.concurrent.CompletableFutures;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.description.DescriptionProvider;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Named
@Path("/suggestions")
@Slf4j
public class Suggestions {
	TitleProvider titles;

	DescriptionProvider descriptions;

	List<ScoresProvider> scoresProviders;

	@Inject
	public Suggestions(TitleProvider titles, DescriptionProvider descriptions, List<ScoresProvider> scoresProviders) {
		this.titles = titles;
		this.descriptions = descriptions;
		this.scoresProviders = scoresProviders;
	}

	@GET
	@Produces("application/json")
	@ManagedAsync
	public void get(@Suspended final AsyncResponse asyncResponse) {
		log.info("get");

		titles.streamOfTitles()
				.map(this::getPage)
				.reduce(
						completedFuture(Stream.<Film>empty()),
						(cf1, cf2) -> cf1.thenCombine(cf2, Stream::concat)
				)
				.thenApply(stream -> stream.collect(toList()))
				.thenApply(asyncResponse::resume)
				.exceptionally(e -> {
					log.error("get - Could not get suggestions", e);
					return asyncResponse.resume(Response.status(INTERNAL_SERVER_ERROR).entity(e).build());
				});

		asyncResponse.setTimeout(15_000, TimeUnit.MILLISECONDS);
		asyncResponse.setTimeoutHandler(ar -> ar.resume(
				Response.status(SERVICE_UNAVAILABLE).entity("Request timed out").build()));

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
}
