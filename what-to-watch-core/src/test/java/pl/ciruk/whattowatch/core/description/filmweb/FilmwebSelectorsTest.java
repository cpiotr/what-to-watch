package pl.ciruk.whattowatch.core.description.filmweb;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilmwebSelectorsTest {
    private final FilmwebSelectors selectors = new FilmwebSelectors();

    @Test
    void shouldFindPlotWithoutLinkToFullDescription() {
        var page = Jsoup.parse("""
                <div class="filmPosterSection__container">
                  <div class="filmPosterSection__plot clamped">
                    <span class="description" itemprop="description">Marty, w którego marzenia nikt nie wierzy.</span>
                    <a href="/film/Wielki+Marty-2025-10056999/descs" class="moreButton">Zobacz pełny opis</a>
                  </div>
                </div>""");

        assertThat(selectors.findPlot(page)).hasValue("Marty, w którego marzenia nikt nie wierzy.");
    }

    @Test
    void shouldFindNoPlotWhenFilmHasNone() {
        var page = Jsoup.parse("""
                <div class="filmPosterSection__container">
                  <div class="filmPosterSection__plot filmPosterSection__plotEmpty">
                    <div class="filmPosterSection__plotGroup">
                      <span data-i18n="film:plot.film.label.empty">Ten film nie ma jeszcze zarysu fabuły.</span>
                      <button type="button" class="filmInfo__linkAdd"><span>dodaj fabułę</span></button>
                    </div>
                  </div>
                </div>""");

        assertThat(selectors.findPlot(page)).isEmpty();
    }
}
