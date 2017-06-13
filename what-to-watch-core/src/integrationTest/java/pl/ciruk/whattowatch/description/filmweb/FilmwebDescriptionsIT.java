package pl.ciruk.whattowatch.description.filmweb;

import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.description.Description;
import pl.ciruk.whattowatch.source.FilmwebProxy;
import pl.ciruk.whattowatch.title.Title;

import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static pl.ciruk.whattowatch.description.DescriptionMatchers.ofTitle;

public class FilmwebDescriptionsIT {

    private FilmwebDescriptions descriptions;

    @Before
    public void setUp() throws Exception {
        JsoupConnection connection = new JsoupConnection(new HtmlConnection(OkHttpClient::new));
        descriptions = new FilmwebDescriptions(new FilmwebProxy(connection), Executors.newSingleThreadExecutor());
    }

    @Test
    public void shouldResolveRamboTitleToFirstBlood() throws Exception {
        Title rambo = Title.builder().title("Rambo").year(1982).build();

        Description description = descriptions.descriptionOf(rambo)
                .orElseThrow(AssertionError::new);

        assertThat(description, is(ofTitle("First Blood")));
    }

    @Test
    public void shouldResolveRecentTitle() throws Exception {
        Title rambo = Title.builder().title("A United Kingdom").year(2016).build();

        Description description = descriptions.descriptionOf(rambo)
                .orElseThrow(AssertionError::new);

        assertThat(description, is(ofTitle("A United Kingdom")));
    }
}