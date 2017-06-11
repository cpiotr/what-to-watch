package pl.ciruk.whattowatch.score.imdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.okhttp.HttpUrl;
import lombok.extern.slf4j.Slf4j;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.stream.Optionals;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.score.Score;
import pl.ciruk.whattowatch.score.ScoresProvider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

@Slf4j
public class ImdbScores implements ScoresProvider {

    public static final int MAX_IMDB_SCORE = 10;
    private final HttpConnection<JsonNode> httpConnection;
    private final ExecutorService executorService;

    public ImdbScores(HttpConnection<JsonNode> httpConnection, ExecutorService executorService) {
        this.httpConnection = httpConnection;
        this.executorService = executorService;
    }

    @Override
    public CompletableFuture<Stream<Score>> scoresOfAsync(Description description) {
        return CompletableFuture.supplyAsync(
                () -> scoresOf(description),
                executorService
        );
    }

    @Override
    public Stream<Score> scoresOf(Description description) {
        log.debug("scoresOf - Description: {}", description);

        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host("www.omdbapi.com")
                .addQueryParameter("t", description.titleAsText())
                .addQueryParameter("y", String.valueOf(description.getYear()))
                .addQueryParameter("plot", "short")
                .addQueryParameter("r", "json")
                .build();

        Optional<Score> json = httpConnection.connectToAndGet(url.toString())
                .filter(this::isResponseSuccessful)
                .flatMap(this::retrieveImdbScore);

        return Optionals.asStream(json);

    }

    private boolean isResponseSuccessful(JsonNode json) {
        return !json.path("Response").asText("").equalsIgnoreCase("False")
                && !json.hasNonNull("Error");
    }

    Optional<Score> retrieveImdbScore(JsonNode json) {
        try {
            double rating = asPercentage(json.path("imdbRating").asDouble());
            int quantity = Integer.parseInt(
                    json.path("imdbVotes").asText().replaceAll("[^0-9]", ""));

            return Optional.of(
                    new Score(rating, quantity, "IMDb"));
        } catch (NumberFormatException e) {
            log.error("retrieveImdbScore - Cannot get score from: {}", json, e);
            return Optional.empty();
        }
    }

    private double asPercentage(double imdbRating) {
        return imdbRating / MAX_IMDB_SCORE;
    }
}
