package pl.ciruk.whattowatch.title.ekino;

import com.codahale.metrics.MetricRegistry;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

public class EkinoTitlesIT {

    private TitleProvider provider;

    @Test
    public void shouldFetchTitlesForValidUser() {
        provider = new EkinoTitles(TestConnections.jsoup(), 1, mock(MetricRegistry.class));

        Stream<Title> titles = provider.streamOfTitles(1);

        Assertions.assertThat(titles).isNotEmpty();
    }
}