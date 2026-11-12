package pl.ciruk.whattowatch.core.score.imdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
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

import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

// Most of the coupling comes from the nested types that map IMDb's search response
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class ImdbSearchScoresProvider implements ScoresProvider {
    private static final int NUMBER_OF_VOTES_LOWER_BOUND = ImdbDatasetScoresProvider.NUMBER_OF_VOTES_LOWER_BOUND;
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_IMDB_SCORE = 10;
    private static final String IMDB = "IMDb";
    private static final int MAX_PAGE_ATTEMPTS = 3;
    private static final Duration DELAY_BETWEEN_PAGE_ATTEMPTS = Duration.ofSeconds(5);
    private final HttpConnection<Element> httpConnection;
    private final Duration delayBetweenPageAttempts;
    private final Counter missingScores = Metrics.counter(
            Names.getNameForMissingScores(),
            List.of(Tags.getProviderTag(IMDB)));
    private final ObjectMapper objectMapper;

    public ImdbSearchScoresProvider(HttpConnection<Element> httpConnection) {
        this(httpConnection, DELAY_BETWEEN_PAGE_ATTEMPTS);
    }

    ImdbSearchScoresProvider(HttpConnection<Element> httpConnection, Duration delayBetweenPageAttempts) {
        this.httpConnection = httpConnection;
        this.delayBetweenPageAttempts = delayBetweenPageAttempts;
        this.objectMapper = new ObjectMapper();
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
            missingScores.increment();
        }

        return firstResult.stream()
                .peek(score -> LOGGER.debug("Score for {}: {}", description, score));
    }

    private Optional<Element> getPage(Description description) {
        // IMDb sometimes serves a page without search data, such as a bot challenge.
        // Another URL (createUrl varies it) usually gets a real one.
        for (int attempt = 1; attempt <= MAX_PAGE_ATTEMPTS; attempt++) {
            var page = httpConnection.get(createUrl(description));
            if (page.isEmpty() || hasSearchData(page.get())) {
                return page;
            }
            LOGGER.debug("No search data for {}, attempt {} of {}", description, attempt, MAX_PAGE_ATTEMPTS);
            if (attempt < MAX_PAGE_ATTEMPTS) {
                try {
                    Thread.sleep(delayBetweenPageAttempts);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return Optional.empty();
    }

    private static boolean hasSearchData(Element page) {
        return !page.select("#__NEXT_DATA__").isEmpty();
    }

    private static HttpUrl createUrl(Description description) {
        var year = description.getYear();
        int offset = ThreadLocalRandom.current().nextInt(NUMBER_OF_VOTES_LOWER_BOUND);
        return createUrlBuilder()
                .addPathSegment("search")
                .addPathSegment("title")
                .addQueryParameter("title", description.titleAsText())
                .addQueryParameter("release_date", String.format("%d,%d", year - 1, year))
                .addQueryParameter("num_votes", String.format("%d,", NUMBER_OF_VOTES_LOWER_BOUND + offset))
                .addQueryParameter("title_type", "feature,tv_movie,video,short")
                .build();
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
            throw new UncheckedIOException(e);
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
