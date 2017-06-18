package pl.ciruk.whattowatch.title.ekino;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.stream.Optionals;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;

@Slf4j
public class EkinoTitles implements TitleProvider {

    private static final String BASE_URL = "http://ekino-tv.pl";

    private static final String TITLES_PAGE_PATTERN = BASE_URL + "/movie/cat/strona%%5B%d%%5D+";

    private final HttpConnection<Element> connection;

    private final int crawledPagesLimit;

    private final AtomicLong numberOfTitles = new AtomicLong();

    public EkinoTitles(HttpConnection<Element> connection, int crawledPagesLimit, MetricRegistry metricRegistry) {
        this.connection = connection;
        this.crawledPagesLimit = crawledPagesLimit;

        metricRegistry.register(
                name(EkinoTitles.class, "numberOfTitles"),
                (Gauge<Long>) numberOfTitles::get);
    }

    @PostConstruct
    public void init() {
        log.info("Crawled pages limit: {}", crawledPagesLimit);
    }

    @Override
    public Stream<Title> streamOfTitles() {
        log.debug("streamOfTitles");

        return generateSomePages(TITLES_PAGE_PATTERN)
                .peek(url -> log.debug("Loading films from: {}", url))
                .map(connection::connectToAndGet)
                .flatMap(Optionals::asStream)
                .flatMap(EkinoStreamSelectors.TITLE_LINKS::extractFrom)
                .map(this::parseToTitle)
                .peek(__ -> numberOfTitles.incrementAndGet());
    }

    private Stream<String> generateSomePages(String pattern) {
        if (pattern.contains("%d")) {
            AtomicInteger i = new AtomicInteger(0);
            return Stream.generate(() -> String.format(pattern, i.incrementAndGet()))
                    .limit(crawledPagesLimit);
        } else {
            return Stream.of(pattern);
        }
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
