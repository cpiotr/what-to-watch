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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class OneTwoThreeTitleProvider implements TitleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BASE_URL = "https://w1.123movie.cc/";

    private static final String TITLES_PAGE_PATTERN = BASE_URL + "movies/page/%d/";

    private final HttpConnection<Element> listConnection;
    private final HttpConnection<Element> detailsConnection;
    private final int pagesPerRequest;

    private final AtomicLong numberOfTitles = new AtomicLong();

    public OneTwoThreeTitleProvider(
            HttpConnection<Element> listConnection,
            HttpConnection<Element> detailsConnection,
            int pagesPerRequest) {
        this.listConnection = listConnection;
        this.detailsConnection = detailsConnection;
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

        return generatePageUrlsForRequest(pageNumber)
                .peek(url -> LOGGER.debug("Loading films from: {}", url))
                .map(listConnection::connectToAndGet)
                .flatMap(Optional::stream)
                .flatMap(OneTwoThreeStreamSelectors.TITLE_LINKS::extractFrom)
                .map(this::visitAndParseToTitle)
                .flatMap(Optional::stream)
                .peek(ignored -> numberOfTitles.incrementAndGet());
    }

    private void logConfiguration() {
        LOGGER.info("Pages per request: {}", pagesPerRequest);
    }

    private Stream<HttpUrl> generatePageUrlsForRequest(int requestNumber) {
        int startFromPage = (requestNumber - 1) * pagesPerRequest;
        LOGGER.debug("Pages: <{}; {}>", startFromPage, startFromPage + pagesPerRequest);

        List<HttpUrl> urls = IntStream.range(startFromPage, startFromPage + pagesPerRequest)
                .boxed()
                .map(index -> String.format(TITLES_PAGE_PATTERN, index))
                .map(HttpUrl::get)
                .collect(toList());
        return urls.stream();
    }

    private Optional<Title> visitAndParseToTitle(Element linkToTitle) {
        return OneTwoThreeSelectors.HREF.extractFrom(linkToTitle)
                .map(HttpUrl::get)
                .flatMap(this::visitAndParseToTitle);
    }

    private Optional<Title> visitAndParseToTitle(HttpUrl link) {
        LOGGER.debug("Visit: {}", link);
        return detailsConnection.connectToAndGet(link)
                .map(pageWithDetails -> parseToTitle(pageWithDetails, link));
    }

    private Title parseToTitle(Element pageWithDetails, HttpUrl link) {
        var filmContainer = pageWithDetails.select("#contenedor .sheader").first();

        var builder = Title.builder();
        builder.url(link.toString());

        OneTwoThreeSelectors.TITLE.extractFrom(filmContainer)
                .ifPresent(builder::title);
        OneTwoThreeSelectors.ORIGINAL_TITLE.extractFrom(filmContainer)
                .ifPresent(builder::originalTitle);
        OneTwoThreeSelectors.YEAR.extractFrom(filmContainer)
                .map(Integer::parseInt)
                .ifPresent(builder::year);

        return builder.build();
    }
}
