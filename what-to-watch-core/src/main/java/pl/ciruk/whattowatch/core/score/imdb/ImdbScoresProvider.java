package pl.ciruk.whattowatch.core.score.imdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;
import pl.ciruk.whattowatch.utils.net.HttpConnection;
import pl.ciruk.whattowatch.utils.text.NumberToken;
import pl.ciruk.whattowatch.utils.text.NumberTokenizer;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.core.score.imdb.ImdbSelectors.*;
import static pl.ciruk.whattowatch.core.score.imdb.ImdbStreamSelectors.FILMS_FROM_SEARCH_RESULT;

public class ImdbScoresProvider implements ScoresProvider {
    static final int NUMBER_OF_VOTES_LOWER_BOUND = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_IMDB_SCORE = 10;
    private static final String IMDB = "IMDb";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final HttpConnection<Element> httpConnection;
    private final ExecutorService executorService;
    private final AtomicLong missingScores = new AtomicLong();

    public ImdbScoresProvider(
            HttpConnection<Element> httpConnection,
            ExecutorService executorService) {
        this.httpConnection = httpConnection;
        this.executorService = executorService;

        Metrics.gauge(
                Names.getNameForMissingScores(),
                List.of(Tags.getProviderTag(IMDB)),
                missingScores,
                AtomicLong::get
        );
    }

    @Override
    public CompletableFuture<Stream<Score>> findScoresByAsync(Description description) {
        return CompletableFuture.supplyAsync(
                () -> findScoresBy(description),
                executorService
        );
    }

    @Override
    public Stream<Score> findScoresBy(Description description) {
        LOGGER.debug("Description: {}", description);

        var year = description.getYear();
        var url = createUrlBuilder()
                .addPathSegment("search")
                .addPathSegment("title")
                .addQueryParameter("title", description.titleAsText())
                .addQueryParameter("release_date", String.format("%d,%d", year - 1, year + 1))
                .addQueryParameter("num_votes", String.format("%d,", NUMBER_OF_VOTES_LOWER_BOUND))
                .addQueryParameter("title_type", "feature,tv_movie,documentary,video")
                .build();

        var firstResult = httpConnection.connectToAndGet(url, "<div class=\"article\"", "<div id=\"sidebar")
                .flatMap(searchResults -> findFirstResult(searchResults, description))
                .flatMap(this::extractScore);
        if (firstResult.isEmpty()) {
            LOGGER.warn("Missing score for {}", description);
            LOGGER.trace("Search query: {}", url);
            missingScores.incrementAndGet();
        }

        return firstResult.stream()
                .peek(score -> LOGGER.debug("Score for {}: {}", description, score));
    }

    private Optional<Element> findFirstResult(Element searchResults, Description description) {
        return FILMS_FROM_SEARCH_RESULT.extractFrom(searchResults)
                .limit(3)
                .filter(searchResult -> matchesTitleFromDescription(searchResult, description))
                .findAny();
    }

    private boolean matchesTitleFromDescription(Element searchResult, Description description) {
        var descriptionTitle = description.getTitle();
        Element summary = searchResult.selectFirst(".lister-item-content");
        return extractTitleFrom(summary).matches(descriptionTitle)
                || extractFullTitleFrom(summary).matches(descriptionTitle);
    }

    private Title extractTitleFrom(Element searchResult) {
        return extractTitleBuilderFrom(searchResult)
                .build();
    }

    private Title extractFullTitleFrom(Element searchResult) {
        return extractTitleBuilderFrom(searchResult)
                .originalTitle(getOriginalTitle(searchResult).orElse(""))
                .build();
    }

    private Title.TitleBuilder extractTitleBuilderFrom(Element searchResult) {
        return Title.builder()
                .title(TITLE.extractFrom(searchResult).orElse(""))
                .year(extractYearFrom(searchResult).orElse(Title.MISSING_YEAR));
    }

    private Optional<String> getOriginalTitle(Element searchResult) {
        return ImdbSelectors.LINK_FROM_SEARCH_RESULT.extractFrom(searchResult)
                .flatMap(this::getDetails)
                .flatMap(this::extractTitle);
    }

    private Optional<String> extractTitle(Element pageWithDetails) {
        return extractTitleFromLdJsonScript(pageWithDetails)
                .or(() -> ORIGINAL_TITLE.extractFrom(pageWithDetails));
    }

    private Optional<String> extractTitleFromLdJsonScript(Element pageWithDetails) {
        return pageWithDetails.select("script")
                .stream()
                .filter(e -> "application/ld+json".equals(e.attr("type")))
                .map(this::extractNameFromJson)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private String extractNameFromJson(Element jsonElement) {
        var json = jsonElement.html();
        try {
            return (String) OBJECT_MAPPER.readValue(json, Map.class).get("name");
        } catch (JsonProcessingException e) {
            LOGGER.warn("Could not parse JSON: {}", json.substring(0, Math.min(20, json.length())));
            return null;
        }
    }

    private Optional<Element> getDetails(String linkToDetails) {
        var url = createUrlBuilder().build().resolve(linkToDetails);

        return httpConnection.connectToAndGet(url, "<script", "<div class=\"SubNav");
    }

    private Optional<Integer> extractYearFrom(Element searchResult) {
        return YEAR.extractFrom(searchResult).map(Integer::parseInt);
    }

    private Optional<Score> extractScore(Element searchResult) {
        var grade = SCORE.extractFrom(searchResult)
                .map(NumberTokenizer::new)
                .filter(NumberTokenizer::hasMoreTokens)
                .map(NumberTokenizer::nextToken)
                .map(NumberToken::asNormalizedDouble)
                .orElse(-1.0);
        var quantity = NUMBER_OF_SCORES.extractFrom(searchResult)
                .map(NumberTokenizer::new)
                .filter(NumberTokenizer::hasMoreTokens)
                .map(NumberTokenizer::nextToken)
                .map(NumberToken::asSimpleLong)
                .orElse(-1L);

        var imdbScore = Score.builder()
                .grade(asPercentage(grade))
                .quantity(quantity)
                .source(IMDB)
                .type(ScoreType.AMATEUR)
                .url(extractLink(searchResult))
                .build();
        return Optional.of(imdbScore)
                .filter(score -> score.grade() > 0.0)
                .filter(score -> score.quantity() > 0);
    }

    private String extractLink(Element searchResult) {
        return ImdbSelectors.LINK_FROM_SEARCH_RESULT.extractFrom(searchResult)
                .map(link -> createUrlBuilder().build().resolve(link))
                .map(HttpUrl::toString)
                .orElse(null);
    }

    private double asPercentage(double imdbRating) {
        return imdbRating / MAX_IMDB_SCORE;
    }

    private static HttpUrl.Builder createUrlBuilder() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("www.imdb.com");
    }
}
