package pl.ciruk.whattowatch.title.zalukaj;

import com.squareup.okhttp.OkHttpClient;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;
import pl.ciruk.core.net.AllCookies;
import pl.ciruk.core.net.HtmlConnection;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.net.html.JsoupConnection;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class ZalukajTitlesIT {

	private TitleProvider provider;
	private Properties properties;

	@Before
	public void setUp() throws Exception {
		properties = loadDevProperties();
	}

	@Test
	public void shouldFetchTitlesForValidUser() throws Exception {
		givenCredentialsArePresent();

		List<Title> titles = provider.streamOfTitles()
				.collect(toList());

		assertThat(titles, is(not(empty())));
	}

	@Test
	public void shouldFetchTitlesForAnonymousUser() throws Exception {
		givenCredentialsAreMissing();

		List<Title> titles = provider.streamOfTitles()
				.collect(toList());

		assertThat(titles, is(not(empty())));
	}

	private void givenCredentialsArePresent() {
		provider = new ZalukajTitles(
				createDirectConnectionWhichKeepsCookies(),
				properties.getProperty("zalukaj-login"),
				properties.getProperty("zalukaj-password")
		);
	}

	private void givenCredentialsAreMissing() {
		provider = new ZalukajTitles(
				createDirectConnectionWhichKeepsCookies(),
				null,
				null
		);
	}

	private static HttpConnection<Element> createDirectConnectionWhichKeepsCookies() {
		OkHttpClient httpClient = new OkHttpClient();
		new AllCookies().applyTo(httpClient);
		HtmlConnection connection = new HtmlConnection(httpClient);
		connection.init();
		return new JsoupConnection(connection);
	}

	private static Properties loadDevProperties() {
		Properties properties = new Properties();
		try {
			properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application-dev.properties"));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return properties;
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