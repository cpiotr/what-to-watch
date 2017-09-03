package pl.ciruk.whattowatch.title.ekino;

import com.codahale.metrics.MetricRegistry;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import pl.ciruk.core.net.Connections;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class EkinoTitlesIT {

    private TitleProvider provider;

    @Test
    public void shouldFetchTitlesForValidUser() throws Exception {
        givenCredentialsArePresent();

        List<Title> titles = provider.streamOfTitles(1)
                .collect(toList());

        assertThat(titles, is(not(empty())));
    }

    private void givenCredentialsArePresent() {
        provider = new EkinoTitles(Connections.jsoup(), 1, mock(MetricRegistry.class));
    }

    private <T> Matcher<Collection<T>> empty() {
        return new TypeSafeMatcher<Collection<T>>() {
            @Override
            protected boolean matchesSafely(Collection<T> item) {
                return item == null || item.isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("empty collection");
            }
        };
    }
}