package pl.ciruk.whattowatch.core.title.justwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.net.TestConnections;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JustWatchTitleProviderIntegrationTest {
    private TitleProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JustWatchTitleProvider(TestConnections.okHttp(), List.of("nfx", "prv"));
    }

    @Test
    void shouldProvidePageOfFilmsOnNetflixAndPrimeVideo() {
        var titles = provider.streamOfTitles(1).toList();

        assertThat(titles).hasSize(100)
                .allSatisfy(title -> {
                    assertThat(title.localTitle()).isNotBlank();
                    assertThat(title.url()).startsWith("https://");
                });
    }

    @Test
    void shouldProvideDifferentTitlesOnNextPage() {
        var firstPage = provider.streamOfTitles(1).map(Title::url).toList();
        var secondPage = provider.streamOfTitles(2).map(Title::url).toList();

        assertThat(secondPage).isNotEmpty().doesNotContainAnyElementsOf(firstPage);
    }
}
