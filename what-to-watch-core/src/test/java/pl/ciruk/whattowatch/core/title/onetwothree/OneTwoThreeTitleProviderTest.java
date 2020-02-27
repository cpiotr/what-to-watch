package pl.ciruk.whattowatch.core.title.onetwothree;

import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.utils.net.TestConnections;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OneTwoThreeTitleProviderTest {

    private OneTwoThreeTitleProvider titleProvider;

    @BeforeEach
    void setUp() {
        titleProvider = new OneTwoThreeTitleProvider(TestConnections.jsoup(), TestConnections.jsoup(), 3);
    }

    @Test
    void shouldGeneratePageUrlsForFirstRequest() {
        var urls = titleProvider.generatePageUrlsForRequest(1).collect(Collectors.toList());

        assertThat(urls)
                .extracting(HttpUrl::encodedPath)
                .containsExactly(firstPagePath(), pathForIndex(2), pathForIndex(3));
    }

    @Test
    void shouldGeneratePageUrlsForSecondRequest() {
        var urls = titleProvider.generatePageUrlsForRequest(2).collect(Collectors.toList());

        assertThat(urls)
                .extracting(HttpUrl::encodedPath)
                .containsExactly(pathForIndex(4), pathForIndex(5), pathForIndex(6));
    }

    @NotNull
    private String firstPagePath() {
        return "/movies";
    }

    @NotNull
    private String pathForIndex(int index) {
        return String.format("/movies/page/%d/", index);
    }
}
