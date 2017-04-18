package pl.ciruk.whattowatch.score.google;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.html.JsoupConnection;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.ciruk.whattowatch.Resources.readContentOf;

public class GoogleSelectorsTest {

    private Document document;

    @Before
    public void setUp() throws Exception {
        document = Jsoup.parse(readContentOf("google-search-results.html"));

        JsoupConnection connection = mock(JsoupConnection.class);
        when(connection.connectToAndGet(any())).thenReturn(Optional.of(document));
    }

    @Test
    public void shouldExtractScoreDescription() throws Exception {
        String scoreDescription = GoogleSelectors.SCORE
                .extractFrom(document)
                .orElseThrow(AssertionError::new);

        assertThat(scoreDescription, containsNumericScore());

    }

    private Matcher<? super String> containsNumericScore() {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return Pattern.matches(".*[0-9],[0-9]+/10.*", item);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("contains score which matches format: X,X/10");
            }
        };
    }
}