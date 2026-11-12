package pl.ciruk.whattowatch.core.title.ekino;

import io.micrometer.core.instrument.Counter;
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
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class EkinoTitleProvider implements TitleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String BASE_URL = "https://ekino.ws";
    private static final String TITLES_PAGE_PATTERN = BASE_URL + "/movie/cat/strona%%5B%d%%5D+";
    private static final Pattern YEAR = Pattern.compile("\\b\\d{4}\\b");
    private static final String LABEL_SEPARATOR = " - ";

    private final HttpConnection<Element> connection;
    private final int pagesPerRequest;
    private final Counter numberOfTitles = Metrics.counter(
            Names.createName(TitleProvider.class, List.of("count")),
            List.of(Tags.getProviderTag("ekino")));

    public EkinoTitleProvider(HttpConnection<Element> connection, int pagesPerRequest) {
        this.connection = connection;
        this.pagesPerRequest = pagesPerRequest;

        LOGGER.info("Pages per request: {}", pagesPerRequest);
    }

    @Override
    public Stream<Title> streamOfTitles(int pageNumber) {
        LOGGER.debug("Page number: {}", pageNumber);

        return pageUrlsFor(pageNumber)
                .peek(url -> LOGGER.debug("Loading films from: {}", url))
                .map(connection::get)
                .flatMap(Optional::stream)
                .flatMap(page -> page.select(".mainWrap .movies-list-item").stream())
                .map(this::parseToTitle);
    }

    // Ekino numbers its pages from 1, and answers page 0 with page 1
    private Stream<HttpUrl> pageUrlsFor(int requestNumber) {
        int firstPage = (requestNumber - 1) * pagesPerRequest + 1;
        return IntStream.range(firstPage, firstPage + pagesPerRequest)
                .mapToObj(page -> HttpUrl.get(String.format(TITLES_PAGE_PATTERN, page)));
    }

    private Title parseToTitle(Element film) {
        numberOfTitles.increment();
        var builder = Title.builder();

        var link = film.selectFirst(".title a");
        if (link != null) {
            builder.title(withoutLabel(link.text()))
                    .url(BASE_URL + link.attr("href"));
        }
        Optional.ofNullable(film.selectFirst(".title .blue a"))
                .map(Element::text)
                .ifPresent(builder::originalTitle);
        yearOf(film).ifPresent(builder::year);

        return builder.build();
    }

    // A title ends with a label such as " - HD", and can have a hyphen of its own, as in "Spider-Man"
    private static String withoutLabel(String title) {
        var labelStart = title.lastIndexOf(LABEL_SEPARATOR);
        return labelStart < 0 ? title : title.substring(0, labelStart).strip();
    }

    // The year leads the categories, such as "2015 | Komedia, Dramat", but can be missing
    private static Optional<Integer> yearOf(Element film) {
        var categories = film.selectFirst(".info-categories .cates");
        if (categories == null) {
            return Optional.empty();
        }
        var year = YEAR.matcher(categories.text());
        return year.find() ? Optional.of(Integer.valueOf(year.group())) : Optional.empty();
    }
}
