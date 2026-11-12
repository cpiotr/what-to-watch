package pl.ciruk.whattowatch.core.score.imdb;

import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImdbScoresProviderTest {

    private HttpConnection<Element> connection;
    private ImdbScoresProvider provider;
    private Description description;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(HttpConnection.class);
        provider = new ImdbScoresProvider(connection, Executors.newSingleThreadExecutor(), Duration.ZERO);
        var title = Title.builder()
                .title("Test title")
                .year(2010)
                .build();
        description = Description.builder()
                .foundFor(title)
                .title(title)
                .build();
    }

    @Test
    void shouldGiveUpWhenPageNeverHasSearchData() {
        when(connection.get(any(HttpUrl.class)))
                .thenReturn(Optional.<Element>of(Jsoup.parse("<html><body>Bot challenge</body></html>")));

        assertThat(provider.findScoresBy(description)).isEmpty();
        verify(connection, times(3)).get(any(HttpUrl.class));
    }

    @Test
    void shouldNotRetryWhenConnectionFails() {
        when(connection.get(any(HttpUrl.class)))
                .thenReturn(Optional.empty());

        assertThat(provider.findScoresBy(description)).isEmpty();
        verify(connection).get(any(HttpUrl.class));
    }
}
