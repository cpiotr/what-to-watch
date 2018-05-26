package pl.ciruk.whattowatch.core.title.ekino;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;

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