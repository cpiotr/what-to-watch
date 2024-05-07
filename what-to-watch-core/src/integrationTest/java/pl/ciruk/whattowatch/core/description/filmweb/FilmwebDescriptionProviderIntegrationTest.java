package pl.ciruk.whattowatch.core.description.filmweb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.concurrent.Executors;

import static pl.ciruk.whattowatch.core.description.DescriptionAssert.assertThat;

class FilmwebDescriptionProviderIntegrationTest {
    private FilmwebDescriptionProvider descriptions;

    @BeforeEach
    void setUp() {
        JsoupConnection connection = TestConnections.jsoup();
        descriptions = new FilmwebDescriptionProvider(
                new FilmwebProxy(connection),
                Executors.newSingleThreadExecutor());
    }

    @Test
    void shouldResolveRamboTitleToFirstBlood() {
        Title title = Title.builder().title("Rambo: Pierwsza krew").year(1982).build();

        Description description = descriptions.findDescriptionBy(title)
                .orElseThrow(AssertionError::new);

        assertThat(description).hasTitle("First Blood");
    }

    @Test
    void shouldResolveRecentTitle() {
        Title title = Title.builder().title("Na noże").year(2019).build();

        Description description = descriptions.findDescriptionBy(title)
                .orElseThrow(AssertionError::new);

        assertThat(description)
                .hasTitle("Knives out")
                .hasPlot()
                .hasGenre("Dramat")
                .hasGenre("Komedia")
                .hasGenre("Kryminał");
    }

    @Test
    void shouldResolveTitleWhenYearIsOffByOne() {
        Title title = Title.builder().title("He's out there").year(2017).build();

        Description description = descriptions.findDescriptionBy(title)
                .orElseThrow(AssertionError::new);

        assertThat(description).hasTitle("He's out there");
    }

    @Test
    void shouldResolveDescriptionFromOriginalTitle() {
        Title title = Title.builder().title("Pasażer - HD").originalTitle("The Commuter").year(2018).build();

        Description description = descriptions.findDescriptionBy(title)
                .orElseThrow(AssertionError::new);

        assertThat(description).hasTitle("The Commuter");
    }

    @Test
    void shouldResolveDescriptionFromOriginalTitleWhichContainsArticle() {
        Title title = Title.builder().originalTitle("Shaun the Sheep Movie: Farmageddon").year(2019).build();

        Description description = descriptions.findDescriptionBy(title)
                .orElseThrow(AssertionError::new);

        assertThat(description).hasTitle("Baranek Shaun Film. Farmageddon");
    }

    @Test
    void shouldResolveDescriptionFromOriginalTitleWhenDocumentariesWithSimilarTitleWhereReleasedOneYearLater() {
        Title title = Title.builder().originalTitle("Gran Torino").year(2008).build();

        Description description = descriptions.findDescriptionBy(title)
                .orElseThrow(AssertionError::new);

        assertThat(description).hasTitle("Gran Torino");
    }

    @Test
    void shouldNotFindDescriptionForUnknownFilm() {
        Title title = Title.builder().originalTitle("NoWaySuchTitleExistsPC").year(2019).build();

        var description = descriptions.findDescriptionBy(title);

        assertThat(description).isEmpty();
    }
}
