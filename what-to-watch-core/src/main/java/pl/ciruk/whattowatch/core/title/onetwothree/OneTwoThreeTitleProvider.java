package pl.ciruk.whattowatch.core.title.onetwothree;

import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.utils.stream.Functions.consumeNothing;

public class OneTwoThreeTitleProvider implements TitleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String BASE_URL = "https://123moviesjr.cc/";
    private static final String TITLES_URL_PATTERN = "//?page=%d";

    private final HttpConnection<Element> listConnection;
    private final int pagesPerRequest;
    private final AtomicLong numberOfTitles = new AtomicLong();
    private final AtomicReference<HttpUrl> moviesUrl = new AtomicReference<>();

    public OneTwoThreeTitleProvider(HttpConnection<Element> listConnection, int pagesPerRequest) {
        this.listConnection = listConnection;
        this.pagesPerRequest = pagesPerRequest;

        Metrics.gauge(
                Names.createName(TitleProvider.class, List.of("count")),
                List.of(Tags.getProviderTag("123movies")),
                numberOfTitles,
                AtomicLong::get);

        logConfiguration();
    }

    @Override
    public Stream<Title> streamOfTitles(int pageNumber) {
        LOGGER.debug("Page number: {}", pageNumber);

        return generatePageUrlsForRequest(moviesUrl(), pageNumber)
                .peek(url -> LOGGER.debug("Loading films from: {}", url))
                .map(listConnection::connectToAndGet)
                .flatMap(Optional::stream)
                .flatMap(OneTwoThreeStreamSelectors.TITLES::extractFrom)
                .map(this::createTitle)
                .flatMap(Optional::stream)
                .peek(ignored -> numberOfTitles.incrementAndGet());
    }

    private void logConfiguration() {
        LOGGER.info("Pages per request: {}", pagesPerRequest);
    }

    private HttpUrl moviesUrl() {
        return moviesUrl.updateAndGet(url -> url == null ? findMoviesUrl() : url);
    }

    // The site moves between domains and redirects to the current one
    private HttpUrl findMoviesUrl() {
        var url = new AtomicReference<>(HttpUrl.get(BASE_URL).resolve("/movies"));
        listConnection.connectToAndConsume(
                HttpUrl.get(BASE_URL + "/movies"),
                consumeNothing(),
                response -> url.set(response.request().url().resolve("/movies")));
        LOGGER.info("URL: {}", url.get());
        return url.get();
    }

    Stream<HttpUrl> generatePageUrlsForRequest(HttpUrl moviesUrl, int requestNumber) {
        int startFromPage = (requestNumber - 1) * pagesPerRequest + 1;
        LOGGER.debug("Pages: <{}; {})", startFromPage, startFromPage + pagesPerRequest);

        return IntStream.iterate(startFromPage + pagesPerRequest - 1, i -> i >= startFromPage, i -> i - 1)
                .mapToObj(index -> createUrlForPageIndex(moviesUrl, index));
    }

    private static HttpUrl createUrlForPageIndex(HttpUrl moviesUrl, int index) {
        return index == 1
                ? moviesUrl
                : HttpUrl.get(moviesUrl.toString() + String.format(TITLES_URL_PATTERN, index));
    }

    private Optional<Title> createTitle(Element titleElement) {
        var optionalTitle = OneTwoThreeSelectors.TITLE.extractFrom(titleElement);
        if (optionalTitle.isEmpty()) {
            return Optional.empty();
        }
        var builder = Title.builder();
        optionalTitle.ifPresent(builder::title);

        var optionalYear = OneTwoThreeSelectors.YEAR.extractFrom(titleElement)
                .filter(Predicate.not(String::isBlank))
                .map(Integer::parseInt);
        if (optionalYear.isEmpty()) {
            return Optional.empty();
        }
        optionalYear.ifPresent(builder::year);

        OneTwoThreeSelectors.HREF.extractFrom(titleElement)
                .map(link -> HttpUrl.get(BASE_URL).resolve(link))
                .map(HttpUrl::toString)
                .ifPresent(builder::url);
        OneTwoThreeSelectors.ORIGINAL_TITLE.extractFrom(titleElement)
                .ifPresent(builder::originalTitle);

        return Optional.of(builder.build());
    }
}
