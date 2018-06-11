package pl.ciruk.whattowatch.core.description.filmweb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionAssert;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.concurrent.Executors;

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