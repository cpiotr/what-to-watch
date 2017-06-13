package pl.ciruk.whattowatch.title.ekino;

import com.squareup.okhttp.OkHttpClient;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jsoup.nodes.Element;
import org.junit.Test;
import pl.ciruk.core.net.AllCookies;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class EkinoTitlesIT {

    private TitleProvider provider;

    @Test
    public void shouldFetchTitlesForValidUser() throws Exception {
        givenCredentialsArePresent();

        List<Title> titles = provider.streamOfTitles()
                .collect(toList());

        assertThat(titles, is(not(empty())));
    }

    private void givenCredentialsArePresent() {
        provider = new EkinoTitles(createDirectConnectionWhichKeepsCookies(), 1);
    }

    private static HttpConnection<Element> createDirectConnectionWhichKeepsCookies() {
        OkHttpClient httpClient = new OkHttpClient();
        new AllCookies().applyTo(httpClient);
        HtmlConnection connection = new HtmlConnection(() -> httpClient);
        connection.init();
        return new JsoupConnection(connection);
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