package pl.ciruk.whattowatch.description.filmweb;

import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.description.DescriptionAssert;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;

import java.util.concurrent.Executors;

public class FilmwebDescriptionsIT {

    private FilmwebDescriptions descriptions;

    @Before
    public void setUp() {
        JsoupConnection connection = TestConnections.jsoup();
        descriptions = new FilmwebDescriptions(
                new FilmwebProxy(connection),
                Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldResolveRamboTitleToFirstBlood() {
        Title rambo = Title.builder().title("Rambo").year(1982).build();

        Description description = descriptions.descriptionOf(rambo)
                .orElseThrow(AssertionError::new);

        DescriptionAssert.assertThat(description).hasTitle("First Blood");
    }

    @Test
    public void shouldResolveRecentTitle() {
        Title rambo = Title.builder().title("A United Kingdom").year(2016).build();

        Description description = descriptions.descriptionOf(rambo)
                .orElseThrow(AssertionError::new);

        DescriptionAssert.assertThat(description).hasTitle("A United Kingdom");
    }

    @Test
    public void shouldResolveDescriptionFromOriginalTitle() {
        Title title = Title.builder().title("Pasażer - HD").originalTitle("The Commuter").year(2018).build();

        Description description = descriptions.descriptionOf(title)
                .orElseThrow(AssertionError::new);

        DescriptionAssert.assertThat(description).hasTitle("The Commuter");
    }
}