package pl.ciruk.whattowatch.title.ekino;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum EkinoSelectors implements Extractable<Optional<String>> {
	TITLE(film -> Optional.ofNullable(film.select(".title a").first().text())),
	ORIGINAL_TITLE(film -> Optional.ofNullable(film.select(".title .blue a").first().text())),
	HREF(film -> Optional.ofNullable(film.select(".title a").first().attr("href"))),
	YEAR(film -> Optional.ofNullable(film.select(".info-categories .cates").text())
            .map(EkinoSelectors::trimToYear))
	;

	private Function<Element, Optional<String>> extractor;

	private EkinoSelectors(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	@Override
	public Optional<String> extractFrom(Element element) {
		return extractor.apply(element);
	}

	private static String trimToYear(String wholeDescriptionInTitle) {
		int endOfFirstPart = wholeDescriptionInTitle.indexOf('|');
		return wholeDescriptionInTitle.substring(0, endOfFirstPart).trim();
	}
}
