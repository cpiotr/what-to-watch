package pl.ciruk.whattowatch.description.filmweb;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.description.DescriptionAssert;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;

import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;

public class FilmwebDescriptionsIT {

    private FilmwebDescriptions descriptions;

    @Before
    public void setUp() {
        JsoupConnection connection = TestConnections.jsoup();
        descriptions = new FilmwebDescriptions(
                new FilmwebProxy(connection),
                mock(MetricRegistry.class),
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
}