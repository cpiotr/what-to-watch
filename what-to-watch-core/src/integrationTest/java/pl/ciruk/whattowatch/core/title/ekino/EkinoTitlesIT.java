package pl.ciruk.whattowatch.core.title.ekino;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.net.TestConnections;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EkinoTitlesIT {

    private TitleProvider provider;

    @BeforeEach
    void setUp() {
        provider = new EkinoTitles(TestConnections.jsoup(), 1);
    }

    @Test
    void shouldFetchTitlesForValidUser() {

        Stream<Title> titles = provider.streamOfTitles(1);

        assertThat(titles).isNotEmpty();
    }
}