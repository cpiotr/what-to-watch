package pl.ciruk.whattowatch.core.score.metacritic;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.utils.net.html.Extractable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public enum MetacriticStreamSelectors implements Extractable<Stream<Element>> {
    SEARCH_RESULTS(
            page -> page.select("ul.search_results li.result").stream()
    ),
    CRITIC_REVIEWS(
            page -> {
                final var html = page.html();
                final var from = html.indexOf("meta={");
                final var to = html.indexOf("</script>", from);
                final var content = html.substring(from, to);// score:93,metaScore:f
                return Stream.generate(new ScoreSupplier(content)).takeWhile(Objects::nonNull);
            }
    );

    private final Function<Element, Stream<Element>> extractor;

    MetacriticStreamSelectors(Function<Element, Stream<Element>> extractor) {
        this.extractor = extractor;
    }

    @Override
    public Stream<Element> extractFrom(Element element) {
        return extractor.apply(element);
    }

    static class ScoreSupplier implements Supplier<Element> {
        private final String content;
        private int index;

        ScoreSupplier(String content) {
            this.content = content;
        }

        @Override
        public Element get() {
            final var next = content.indexOf("platform:", index);
            if (next < index) return null;
            index = next;
            final var from = content.indexOf("score:", index);
            var i = from + "score:".length();
            int score = 0;
            while (Character.isDigit(content.charAt(i))) {
                score = score * 10;
                score += content.charAt(i) - '0';
                i++;
            }
            index = i;
            final var nameFrom = content.indexOf("publicationName:\"", index) + "publicationName:\"".length();
            if (nameFrom < index) return null;
            final var nameTo = content.indexOf('"', nameFrom + 1);
            final var name = content.substring(nameFrom, nameTo);
            index = nameTo;
            final var p = new Element("p");
            p.appendChild(new Element("div").appendText(String.valueOf(score)));
            p.appendChild(new Element("span").appendText(name));
            return p;
        }
    }

}
