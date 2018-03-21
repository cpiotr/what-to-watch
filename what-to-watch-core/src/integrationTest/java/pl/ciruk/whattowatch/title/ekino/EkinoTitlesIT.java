package pl.ciruk.whattowatch.title.ekino;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import pl.ciruk.core.net.TestConnections;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.util.stream.Stream;

public class EkinoTitlesIT {

    private TitleProvider provider;

    @Test
    public void shouldFetchTitlesForValidUser() {
        provider = new EkinoTitles(TestConnections.jsoup(), 1);

        Stream<Title> titles = provider.streamOfTitles(1);

        Assertions.assertThat(titles).isNotEmpty();
    }
}