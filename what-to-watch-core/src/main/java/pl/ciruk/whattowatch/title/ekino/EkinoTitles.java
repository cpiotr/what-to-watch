package pl.ciruk.whattowatch.title.ekino;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;

@Slf4j
public class EkinoTitles implements TitleProvider {

    private static final String BASE_URL = "http://ekino-tv.pl";

    private static final String TITLES_PAGE_PATTERN = BASE_URL + "/movie/cat/strona%%5B%d%%5D+";

    private final HttpConnection<Element> connection;

    private final int pagesPerRequest;

    private final AtomicLong numberOfTitles = new AtomicLong();

    public EkinoTitles(HttpConnection<Element> connection, int pagesPerRequest, MetricRegistry metricRegistry) {
        this.connection = connection;
        this.pagesPerRequest = pagesPerRequest;

        metricRegistry.register(
                name(EkinoTitles.class, "numberOfTitles"),
                (Gauge<Long>) numberOfTitles::get);

        logConfiguration();
    }

    private void logConfiguration() {
        log.info("Pages per request: {}", pagesPerRequest);
    }

    @Override
    public Stream<Title> streamOfTitles(int pageNumber) {
        log.debug("streamOfTitles - Page number: {}", pageNumber);

        return generatePageUrlsForRequest(pageNumber)
                .peek(url -> log.debug("Loading films from: {}", url))
                .map(connection::connectToAndGet)
                .flatMap(Optional::stream)
                .flatMap(EkinoStreamSelectors.TITLE_LINKS::extractFrom)
                .map(this::parseToTitle)
                .peek(__ -> numberOfTitles.incrementAndGet());
    }

    private Stream<String> generatePageUrlsForRequest(int requestNumber) {
        AtomicInteger i = new AtomicInteger(0);
        int startFromPage = (requestNumber - 1) * pagesPerRequest;
        log.debug("generatePageUrlsForRequest - Pages: <{}; {}>", startFromPage, startFromPage + pagesPerRequest);
        return Stream.generate(() -> String.format(TITLES_PAGE_PATTERN, i.incrementAndGet()))
                .skip(startFromPage)
                .limit(pagesPerRequest);
    }

    private Title parseToTitle(Element linkToTitle) {
        Title.TitleBuilder builder = Title.builder();

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
