package pl.ciruk.whattowatch.title.zalukaj;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.html.JsoupConnection;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZalukajSelectorsTest {

    private Document document;

    @Before
    public void setUp() throws Exception {
        String searchResultsHTML = new String(
                Files.readAllBytes(
                        Paths.get(
                                getClass().getClassLoader().getResource("zalukaj-search-results.html").toURI())));
        JsoupConnection connection = mock(JsoupConnection.class);
        document = Jsoup.parse(searchResultsHTML);
        when(connection.connectToAndGet(any())).thenReturn(Optional.of(document));
    }

    @Test
    public void shouldExtractTitlesFromPage() throws Exception {
        List<String> titles = ZalukajStreamSelectors.TITLE_LINKS.extractFrom(document)
                .map(ZalukajSelectors.HREF::extractFrom)
                .map(Optional::get)
                .collect(toList());

        assertThat(titles, everyItem(not(equalTo(""))));
    }

    @Test
    public void titlesShouldNotContainUnrelatedInformation() throws Exception {
        List<String> titles = ZalukajStreamSelectors.TITLE_LINKS.extractFrom(document)
                .map(ZalukajSelectors.TITLE::extractFrom)
                .map(Optional::get)
                .collect(toList());

        assertThat(titles, everyItem(containsOnlyTitleAndYear()));
    }

    private Matcher<String> containsOnlyTitleAndYear() {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return Pattern.matches(".+\\(\\d{4}\\)$", item);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(" contains only title and year");
            }
        };
    }
}