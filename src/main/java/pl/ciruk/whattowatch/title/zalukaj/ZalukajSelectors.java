package pl.ciruk.whattowatch.title.zalukaj;

import org.jsoup.nodes.Element;
import pl.ciruk.core.net.Extractable;

import java.util.function.Function;
import java.util.stream.Stream;

public enum ZalukajSelectors implements Extractable<Stream<String>> {
	TITLES(description -> description.select(".tivief4 .rmk23m4 h3 a")
			.stream()
			.map(Element::text)
	),;

	private Function<Element, Stream<String>> extractor;

	private ZalukajSelectors(Function<Element, Stream<String>> extractor) {
		this.extractor = extractor;
	}

	@Override
	public Stream<String> extractFrom(Element element) {
		return extractor.apply(element);
	}
}
