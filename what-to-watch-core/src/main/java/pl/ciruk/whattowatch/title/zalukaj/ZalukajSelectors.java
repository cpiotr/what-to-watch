package pl.ciruk.whattowatch.title.zalukaj;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.Optional;
import java.util.function.Function;

public enum ZalukajSelectors implements Extractable<Optional<String>> {
	TITLE(link -> Optional.ofNullable(link.attr("title"))
			.map(ZalukajSelectors::onlyTitleAndYear)),
	HREF(link -> Optional.ofNullable(link.attr("href")))
	;

	private Function<Element, Optional<String>> extractor;

	private ZalukajSelectors(Function<Element, Optional<String>> extractor) {
		this.extractor = extractor;
	}

	@Override
	public Optional<String> extractFrom(Element element) {
		return extractor.apply(element);
	}

	private static String onlyTitleAndYear(String wholeDescriptionInTitle) {
		int endOfFirstPart = wholeDescriptionInTitle.indexOf('|');
		return wholeDescriptionInTitle.substring(0, endOfFirstPart).trim();
	}
}
