package pl.ciruk.films.whattowatch.title.ekino;

import static pl.ciruk.core.net.JsoupNodes.emptyTextElement;

import java.util.function.Function;

import org.jsoup.nodes.Element;

public enum EkinoSelector {
	YEAR(details -> details.select(".title p.a a")
			.stream()
			.filter(a -> a.attr("href").contains("year"))
			.findFirst()
			.orElse(emptyTextElement())
			.text()
			.replaceAll("[^0-9]", "")),
	LOCAL_TITLE(details -> details.select(".title h2 a")
			.stream()
			.findFirst()
			.orElse(emptyTextElement())
			.text()),
	ORIGINAL_TITLE(details -> details.select(".title h3 a")
			.stream()
			.findFirst()
			.orElse(emptyTextElement())
			.text()),
	;
	private Function<Element, String> extractor;
	
	private EkinoSelector(Function<Element, String> extractor) {
		this.extractor = extractor;
	}

	public String extractFrom(Element element) {
		return extractor.apply(element);
	}
}
