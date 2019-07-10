package pl.ciruk.whattowatch.core.title.ekino;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.net.TestConnections;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EkinoTitleProviderIT {

    private TitleProvider provider;

    @BeforeEach
    void setUp() {
        provider = new EkinoTitleProvider(TestConnections.jsoup(), 1);
    }

    @Disabled
    @Test
    void shouldProvideCollectionOfTitles() {
        Stream<Title> titles = provider.streamOfTitles(1);

        assertThat(titles).isNotEmpty();
    }
}
