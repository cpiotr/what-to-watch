package pl.ciruk.whattowatch.core.title.onetwothree;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.net.TestConnections;
import pl.ciruk.whattowatch.utils.net.html.JsoupConnection;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
class OneTwoThreeTitleProviderIntegrationTest {
    private TitleProvider provider;

    @BeforeEach
    void setUp() {
        JsoupConnection connection = TestConnections.jsoup();
        provider = new OneTwoThreeTitleProvider(connection, 1);
    }

    @Test
    void shouldFetchTitlesFromFirstPage() {
        var titles = provider.streamOfTitles(1).collect(Collectors.toList());

        assertThat(titles).isNotEmpty();
    }

    @Test
    void shouldFetchTitlesFromSecondPage() {
        var titles = provider.streamOfTitles(2).collect(Collectors.toList());

        assertThat(titles).isNotEmpty();
    }
}
