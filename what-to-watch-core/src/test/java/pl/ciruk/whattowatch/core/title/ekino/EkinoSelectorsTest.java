package pl.ciruk.whattowatch.core.title.ekino;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EkinoSelectorsTest {

    @Test
    void shouldFindYearInDescription() {
        assertThat(EkinoSelectors.YEAR.extractFrom(filmWithDescription("2015 | Komedia, Dramat"))).hasValue("2015");
    }

    @Test
    void shouldFindYearInDescriptionWithoutCategories() {
        assertThat(EkinoSelectors.YEAR.extractFrom(filmWithDescription("2015"))).hasValue("2015");
    }

    @Test
    void shouldFindNoYearWhenDescriptionHasNone() {
        assertThat(EkinoSelectors.YEAR.extractFrom(filmWithDescription("Komedia"))).isEmpty();
        assertThat(EkinoSelectors.YEAR.extractFrom(filmWithDescription("brak | Dramat"))).isEmpty();
    }

    private static Element filmWithDescription(String description) {
        return Jsoup.parse("<div class=\"info-categories\"><div class=\"cates\">" + description + "</div></div>").body();
    }
}
