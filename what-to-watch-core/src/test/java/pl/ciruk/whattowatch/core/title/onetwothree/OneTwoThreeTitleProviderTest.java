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
        titleProvider = new OneTwoThreeTitleProvider(TestConnections.jsoup(), 3);
    }

    @Test
    void shouldGeneratePageUrlsForFirstRequest() {
        var urls = titleProvider.generatePageUrlsForRequest(1).collect(Collectors.toList());

        assertThat(urls)
                .extracting(HttpUrl::encodedQuery)
                .containsExactly(queryForIndex(3), queryForIndex(2), firstPageQuery());
    }

    @Test
    void shouldGeneratePageUrlsForSecondRequest() {
        var urls = titleProvider.generatePageUrlsForRequest(2).collect(Collectors.toList());

        assertThat(urls)
                .extracting(HttpUrl::encodedQuery)
                .containsExactly(queryForIndex(6), queryForIndex(5), queryForIndex(4));
    }

    private String firstPageQuery() {
        return null;
    }

    @NotNull
    private String queryForIndex(int index) {
        return String.format("page=%d", index);
    }
}
