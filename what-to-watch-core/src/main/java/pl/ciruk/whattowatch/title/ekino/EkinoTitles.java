package pl.ciruk.whattowatch.title.ekino;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.stream.Optionals;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
public class EkinoTitles implements TitleProvider {

    public static final String BASE_URL = "http://ekino-tv.pl/";
    private final HttpConnection<Element> connection;

	public EkinoTitles(HttpConnection<Element> connection) {
		this.connection = connection;
	}

	List<String> urls = Lists.newArrayList(
            BASE_URL + "movie/cat/strona%%5B%d%%5D+"
	);

	@Override
	public Stream<Title> streamOfTitles() {
		log.info("streamOfTitles");

		return urls.stream()
				.flatMap(pattern -> generateSomePages(pattern))
				.peek(url -> log.info("Loading films from: {}", url))
				.map(connection::connectToAndGet)
				.flatMap(Optionals::asStream)
				.flatMap(EkinoStreamSelectors.TITLE_LINKS::extractFrom)
				.map(this::parseToTitle)
				.distinct();
	}

	Stream<String> generateSomePages(String pattern) {
		if (pattern.contains("%d")) {
			AtomicInteger i = new AtomicInteger(0);
			return Stream.generate(
					() -> String.format(pattern, i.incrementAndGet()))
					.limit(10);
		} else {
			return Stream.of(pattern);
		}
	}

	Title parseToTitle(Element linkToTitle) {
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
