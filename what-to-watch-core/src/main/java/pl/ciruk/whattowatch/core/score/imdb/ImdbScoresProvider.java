package pl.ciruk.whattowatch.core.score.imdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.core.score.imdb.ImdbSelectors.*;

public class ImdbScoresProvider implements ScoresProvider {
    static final int NUMBER_OF_VOTES_LOWER_BOUND = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_IMDB_SCORE = 10;
    private static final String IMDB = "IMDb";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final HttpConnection<Element> httpConnection;
    private final ExecutorService executorService;
    private final AtomicLong missingScores = new AtomicLong();
    private final ObjectMapper objectMapper;

    public ImdbScoresProvider(
            HttpConnection<Element> httpConnection,
            ExecutorService executorService) {
        this.httpConnection = httpConnection;
        this.executorService = executorService;
        this.objectMapper = new ObjectMapper();

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

        var firstResult = getPage(description)
                .flatMap(searchResults -> findFirstResult(searchResults, description))
                .flatMap(this::extractScore);
        if (firstResult.isEmpty()) {
            LOGGER.warn("Missing score for {}", description);
            LOGGER.trace("Search query: {}", createUrl(description));
            missingScores.incrementAndGet();
        }

        return firstResult.stream()
                .peek(score -> LOGGER.debug("Score for {}: {}", description, score));
    }

    private Optional<Element> getPage(Description description) {
        var url = createUrl(description);
        var element = httpConnection.connectToAndGet(url);
        while (element.stream().flatMap(html -> html.select("#__NEXT_DATA__").stream()).count() == 0) {
            System.out.println("Again...");
            System.out.println(element.get().html());
            url = createUrl(description);
            try {
                Thread.sleep(Duration.of(5, ChronoUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            element = httpConnection.connectToAndGet(url);
        }
        return element;
    }

    private static HttpUrl createUrl(Description description) {
        var year = description.getYear();
        int offset = ((int) (NUMBER_OF_VOTES_LOWER_BOUND * Math.random()));
        var url = createUrlBuilder()
                .addPathSegment("search")
                .addPathSegment("title")
                .addQueryParameter("title", description.titleAsText())
                .addQueryParameter("release_date", String.format("%d,%d", year - 1, year))
                .addQueryParameter("num_votes", String.format("%d,", NUMBER_OF_VOTES_LOWER_BOUND + offset))
                .addQueryParameter("title_type", "feature,tv_movie,video,short")
                .build();
        return url;
    }

    private Optional<TitleItem> findFirstResult(Element searchResults, Description description) {
        // <script id="__NEXT_DATA__"
        // "searchResults":{"titleResults":{"titleListItems":
        // ratingSummary":{"aggregateRating
        final ImdbData imdbData;
        try {
            final var html = searchResults.select("#__NEXT_DATA__").html();
            imdbData = objectMapper.readValue(html, ImdbData.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return imdbData.getProps().getPageProps().getSearchResults().getTitleResults().getTitleListItems().stream()
                .limit(3)
                .filter(searchResult -> matchesTitleFromDescription(searchResult, description))
                .findAny();
    }

    private boolean matchesTitleFromDescription(TitleItem searchResult, Description description) {
        var descriptionTitle = description.getTitle();
        final var title = Title.builder()
                .year(searchResult.getReleaseYear())
                .originalTitle(searchResult.getOriginalTitleText())
                .title(searchResult.getTitleText())
                .build();
        return title.matches(descriptionTitle);
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

    private Optional<Score> extractScore(TitleItem searchResult) {
        var grade = searchResult.getRatingSummary().getAggregateRating();
        var quantity = searchResult.getRatingSummary().getVoteCount();

        var imdbScore = Score.builder()
                .grade(asPercentage(grade))
                .quantity(quantity)
                .source(IMDB)
                .type(ScoreType.AMATEUR)
                .url(createUrlBuilder()
                        .addPathSegment("title")
                        .addPathSegment(searchResult.getTitleId())
                        .build()
                        .toString())
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImdbData {
        Props props;

        public Props getProps() {
            return props;
        }

        public void setProps(Props props) {
            this.props = props;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Props {
        PageProps pageProps;

        public PageProps getPageProps() {
            return pageProps;
        }

        public void setPageProps(PageProps pageProps) {
            this.pageProps = pageProps;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageProps {
        SearchResults searchResults;

        public SearchResults getSearchResults() {
            return searchResults;
        }

        public void setSearchResults(SearchResults searchResults) {
            this.searchResults = searchResults;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResults {
        private TitleResults titleResults;

        public TitleResults getTitleResults() {
            return titleResults;
        }

        public void setTitleResults(TitleResults titleResults) {
            this.titleResults = titleResults;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TitleResults {
        private List<TitleItem> titleListItems;

        public List<TitleItem> getTitleListItems() {
            return titleListItems;
        }

        public void setTitleListItems(List<TitleItem> titleListItems) {
            this.titleListItems = titleListItems;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TitleItem {
        RatingSummary ratingSummary;
        String originalTitleText;
        String titleText;
        int releaseYear;
        int metascore;
        String titleId;

        public RatingSummary getRatingSummary() {
            return ratingSummary;
        }

        public void setRatingSummary(RatingSummary ratingSummary) {
            this.ratingSummary = ratingSummary;
        }

        public String getOriginalTitleText() {
            return originalTitleText;
        }

        public void setOriginalTitleText(String originalTitleText) {
            this.originalTitleText = originalTitleText;
        }

        public String getTitleText() {
            return titleText;
        }

        public void setTitleText(String titleText) {
            this.titleText = titleText;
        }

        public int getReleaseYear() {
            return releaseYear;
        }

        public void setReleaseYear(int releaseYear) {
            this.releaseYear = releaseYear;
        }

        public int getMetascore() {
            return metascore;
        }

        public void setMetascore(int metascore) {
            this.metascore = metascore;
        }

        public String getTitleId() {
            return titleId;
        }

        public void setTitleId(String titleId) {
            this.titleId = titleId;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RatingSummary {
        double aggregateRating;
        int voteCount;

        public double getAggregateRating() {
            return aggregateRating;
        }

        public void setAggregateRating(double aggregateRating) {
            this.aggregateRating = aggregateRating;
        }

        public int getVoteCount() {
            return voteCount;
        }

        public void setVoteCount(int voteCount) {
            this.voteCount = voteCount;
        }
    }
}
