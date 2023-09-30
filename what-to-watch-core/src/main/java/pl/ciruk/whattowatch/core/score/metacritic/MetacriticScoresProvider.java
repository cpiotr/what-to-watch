package pl.ciruk.whattowatch.core.score.metacritic;

import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static pl.ciruk.whattowatch.core.score.metacritic.MetacriticSelectors.LINK_TO_DETAILS;
import static pl.ciruk.whattowatch.core.title.Title.MISSING_YEAR;

public class MetacriticScoresProvider implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final HttpConnection<Element> connection;
    private final ExecutorService executorService;
    private final AtomicLong missingMetacriticScores = new AtomicLong();
    private final AtomicLong missingNewYorkTimesScores = new AtomicLong();

    public MetacriticScoresProvider(
            HttpConnection<Element> connection,
            ExecutorService executorService) {
        this.connection = connection;
        this.executorService = executorService;

        Metrics.gauge(
                Names.getNameForMissingScores(),
                List.of(Tags.getProviderTag(MetacriticScoreUtil.METACRITIC)),
                missingMetacriticScores,
                AtomicLong::get
        );

        Metrics.gauge(
                Names.getNameForMissingScores(),
                List.of(Tags.getProviderTag(NewYorkTimesScoreUtil.NEW_YORK_TIMES)),
                missingNewYorkTimesScores,
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

        return metacriticSummaryOf(description.getTitle())
                .flatMap(LINK_TO_DETAILS::extractFrom) // ,criticScoreSummary:{url:"\u002Fmovie\u002Fmoana\u002Fcritic-reviews\u002F"
                .stream()
                .flatMap(linkToDetails -> findScores(linkToDetails, description));
    }

    private Stream<Score> findScores(String linkToDetails, Description description) {
        var htmlWithScores = getPageWithCriticReviews(List.of(linkToDetails, "critic-reviews"))
                .map(this::extractCriticReviews)
                .or(() -> followDetailsLinkAndFindPageWithScores(linkToDetails));

        var metacriticScoreBuilder = htmlWithScores
                .flatMap(MetacriticScoreUtil::extractToScoreBuilder);
        if (metacriticScoreBuilder.isEmpty()) {
            LOGGER.warn("Missing Metacritic score for: {}", description.getTitle());
            missingMetacriticScores.incrementAndGet();
        }

        var nytScoreBuilder = htmlWithScores.flatMap(NewYorkTimesScoreUtil::extractToScoreBuilder);
        if (nytScoreBuilder.isEmpty()) {
            LOGGER.warn("Missing NYT score for: {}", description.getTitle());
            missingNewYorkTimesScores.incrementAndGet();
        }

        return Stream.concat(metacriticScoreBuilder.stream(), nytScoreBuilder.stream())
                .map(scoreBuilder -> scoreBuilder.url(resolve(linkToDetails)).build())
                .peek(score -> LOGGER.debug("Score for {}: {}", description, score));
    }

    private Optional<Element> followDetailsLinkAndFindPageWithScores(String linkToDetails) {
        final var page = getPage(linkToDetails);
        return page
                .flatMap(MetacriticSelectors.LINK_TO_CRITIC_REVIEWS::extractFrom)
                .flatMap(link -> getPageWithCriticReviews(List.of(link)))
                .map(this::extractCriticReviews);
    }

    private Element extractCriticReviews(Element page) {
        return page.selectFirst("div.critic_reviews");
    }

    private String resolve(String link) {
        HttpUrl httpUrl = metacriticUrlBuilder().build();
        return Optional.ofNullable(httpUrl.resolve(link))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("Could not resolve: " + link));
    }

    private Optional<Element> getSearchResultsFor(Title title) {
        var url = metacriticUrlBuilder()
                .addPathSegments("movie")
                .addPathSegment(title.asText().replace(' ', '-').toLowerCase(Locale.ROOT))
                .addPathSegment("critic-reviews")
                .build();
        return connection.connectToAndGet(url);
    }

    private Optional<Element> getPage(String pathSegment) {
        return getPage(List.of(pathSegment));
    }

    private Optional<Element> getPageWithCriticReviews(List<String> pathSegments) {
        var url = buildUrl(pathSegments);
        return connection.connectToAndGet(url, "<div class=\"critic_reviews", "<div class=\"side_col\"");
    }

    private Optional<Element> getPage(List<String> pathSegments) {
        HttpUrl url = buildUrl(pathSegments);
        return connection.connectToAndGet(url, "<div class=\"content_under_header", "<div class=\"content_after_header");
    }

    private HttpUrl buildUrl(List<String> pathSegments) {
        var builder = metacriticUrlBuilder();
        pathSegments.forEach(builder::addPathSegments);
        return builder.build();
    }

    private HttpUrl.Builder metacriticUrlBuilder() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("www.metacritic.com");
    }

    private Optional<Element> metacriticSummaryOf(Title title) {
        try {
            return getSearchResultsFor(title);
        } catch (Exception e) {
            LOGGER.warn("Cannot find metacritic summary of {}", title, e);
            return Optional.empty();
        }
    }

    private Optional<Element> findFirstResultMatching(Title title, Element searchResults) {
        return MetacriticStreamSelectors.SEARCH_RESULTS.extractFrom(searchResults)
                .map(searchResult -> new SearchResultWithTitle(searchResult, extractTitle(searchResult)))
                .filter(searchResult -> searchResult.matches(title))
                .min(comparing(searchResult -> searchResult.calculateDifferenceInYears(title)))
                .map(SearchResultWithTitle::getResultElement);
    }

    private Title extractTitle(Element searchResult) {
        var title = MetacriticSelectors.TITLE.extractFrom(searchResult).orElse("");
        var year = MetacriticSelectors.RELEASE_YEAR.extractFrom(searchResult)
                .map(Integer::parseInt)
                .orElse(MISSING_YEAR);

        return Title.builder()
                .title(title)
                .year(year)
                .build();
    }

    private record SearchResultWithTitle(Element resultElement, Title title) {
        public int calculateDifferenceInYears(Title otherTitle) {
            return Math.abs(title.year() - otherTitle.year());
        }

        public boolean matches(Title otherTitle) {
            return title.matches(otherTitle);
        }

        public Element getResultElement() {
            return resultElement;
        }

        public Title getTitle() {
            return title;
        }
    }
}
