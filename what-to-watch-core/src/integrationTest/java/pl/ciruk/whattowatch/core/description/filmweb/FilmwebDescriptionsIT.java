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

public class FilmwebDescriptionsIT {

    private FilmwebDescriptions descriptions;

    @BeforeEach
    public void setUp() {
        JsoupConnection connection = TestConnections.jsoup();
        descriptions = new FilmwebDescriptions(
                new FilmwebProxy(connection),
                Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldResolveRamboTitleToFirstBlood() {
        Title title = Title.builder().title("Rambo").year(1982).build();

        Description description = descriptions.findDescriptionOf(title)
                .orElseThrow(AssertionError::new);

        assertThat(description).hasTitle("First Blood");
    }

    @Test
    public void shouldResolveRecentTitle() {
        Title title = Title.builder().title("A United Kingdom").year(2016).build();

        Description description = descriptions.findDescriptionOf(title)
                .orElseThrow(AssertionError::new);

        assertThat(description).hasTitle("A United Kingdom");
    }

    @Test
    public void shouldResolveTitleWhenYearIsOffByOne() {
        Title title = Title.builder().title("He's out there").year(2017).build();

        Description description = descriptions.findDescriptionOf(title)
                .orElseThrow(AssertionError::new);

        assertThat(description).hasTitle("He's out there");
    }

    @Test
    public void shouldResolveDescriptionFromOriginalTitle() {
        Title title = Title.builder().title("Pasażer - HD").originalTitle("The Commuter").year(2018).build();

        Description description = descriptions.findDescriptionOf(title)
                .orElseThrow(AssertionError::new);

        assertThat(description).hasTitle("The Commuter");
    }
}