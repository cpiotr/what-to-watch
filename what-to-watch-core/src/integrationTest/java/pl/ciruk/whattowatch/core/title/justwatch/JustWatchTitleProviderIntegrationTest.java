package pl.ciruk.whattowatch.core.title.justwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.title.TitleKind;
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
        var titles = provider.streamOfTitles(TitleKind.FILM, 1).toList();

        assertThat(titles).hasSize(100)
                .allSatisfy(title -> {
                    assertThat(title.localTitle()).isNotBlank();
                    assertThat(title.url()).startsWith("https://");
                });
    }

    @Test
    void shouldProvidePageOfSeriesOnNetflixAndPrimeVideo() {
        var titles = provider.streamOfTitles(TitleKind.SERIES, 1).toList();

        assertThat(titles).hasSize(100)
                .allSatisfy(title -> {
                    assertThat(title.localTitle()).isNotBlank();
                    assertThat(title.kind()).isEqualTo(TitleKind.SERIES);
                });
    }

    @Test
    void shouldProvideDifferentTitlesOnNextPage() {
        var firstPage = provider.streamOfTitles(TitleKind.FILM, 1).map(Title::url).toList();
        var secondPage = provider.streamOfTitles(TitleKind.FILM, 2).map(Title::url).toList();

        assertThat(secondPage).isNotEmpty().doesNotContainAnyElementsOf(firstPage);
    }
}
