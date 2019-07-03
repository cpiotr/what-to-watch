package pl.ciruk.whattowatch.core.title.onetwothree;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.core.title.ekino.EkinoTitleProvider;
import pl.ciruk.whattowatch.utils.net.TestConnections;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OneTwoThreeTitleProviderIT {

    private TitleProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OneTwoThreeTitleProvider(TestConnections.jsoup(), 1);
    }

    @Test
    void shouldProvideCollectionOfTitles() {
        var titles = provider.streamOfTitles(1).collect(Collectors.toList());

        assertThat(titles).isNotEmpty();
    }
}
