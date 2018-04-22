package pl.ciruk.whattowatch.title.ekino;

import io.micrometer.core.instrument.Metrics;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class EkinoTitles implements TitleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BASE_URL = "http://ekino-tv.pl";

    private static final String TITLES_PAGE_PATTERN = BASE_URL + "/movie/cat/strona%%5B%d%%5D+";

    private final HttpConnection<Element> connection;

    private final int pagesPerRequest;

    private final AtomicLong numberOfTitles = new AtomicLong();

    public EkinoTitles(HttpConnection<Element> connection, int pagesPerRequest) {
        this.connection = connection;
        this.pagesPerRequest = pagesPerRequest;

        Metrics.gauge(
                MethodHandles.lookup().lookupClass().getSimpleName() + "numberOfTitles",
                numberOfTitles,
                AtomicLong::get);

        logConfiguration();
    }

    private void logConfiguration() {
        LOGGER.info("Pages per request: {}", pagesPerRequest);
    }

    @Override
    public Stream<Title> streamOfTitles(int pageNumber) {
        LOGGER.debug("streamOfTitles - Page number: {}", pageNumber);

        return generatePageUrlsForRequest(pageNumber)
                .peek(url -> LOGGER.debug("Loading films from: {}", url))
                .map(connection::connectToAndGet)
                .flatMap(Optional::stream)
                .flatMap(EkinoStreamSelectors.TITLE_LINKS::extractFrom)
                .map(this::parseToTitle)
                .peek(__ -> numberOfTitles.incrementAndGet());
    }

    private Stream<String> generatePageUrlsForRequest(int requestNumber) {
        int startFromPage = (requestNumber - 1) * pagesPerRequest;
        LOGGER.debug("generatePageUrlsForRequest - Pages: <{}; {}>", startFromPage, startFromPage + pagesPerRequest);
        return IntStream.range(startFromPage, startFromPage + pagesPerRequest)
                .boxed()
                .map(index -> String.format(TITLES_PAGE_PATTERN, index));
    }

    private Title parseToTitle(Element linkToTitle) {
        var builder = Title.builder();

        EkinoSelectors.TITLE.extractFrom(linkToTitle)
                .ifPresent(builder::title);
        EkinoSelectors.HREF.extractFrom(linkToTitle)
                .map(link -> BASE_URL + link)
                .ifPresent(builder::url);
        EkinoSelectors.YEAR.extractFrom(linkToTitle)
                .map(Integer::valueOf)
                .ifPresent(builder::year);
        EkinoSelectors.ORIGINAL_TITLE.extractFrom(linkToTitle)
                .ifPresent(builder::originalTitle);

        return builder.build();
    }
}
