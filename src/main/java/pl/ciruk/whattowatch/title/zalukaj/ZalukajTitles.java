package pl.ciruk.whattowatch.title.zalukaj;

import com.squareup.okhttp.FormEncodingBuilder;
import org.springframework.beans.factory.annotation.Value;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.core.stream.Optionals;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Named
public class ZalukajTitles implements TitleProvider {
	public static final Pattern YEAR = Pattern.compile(".*\\(([12][0-9]{3})\\)$");
	public static final Pattern ORIGINAL_TITLE = Pattern.compile("(.*)\\((.*)\\)$");
	private JsoupCachedConnection connection;

	@Inject
	public ZalukajTitles(JsoupCachedConnection connection) {
		this.connection = connection;
	}

	@Value("${zalukaj-login-url}")
	String loginPage;

	@Value("${zalukaj-login}")
	String login;

	@Value("${zalukaj-password}")
	String password;

	@Value("#{'${titles.zalukaj.url-patterns}'.split(';')}")
	List<String> urls;

	@Override
	public Stream<Title> streamOfTitles() {
		connection.connectToAndConsume(
				loginPage,
				request -> request.post(
						new FormEncodingBuilder()
								.add("login", login)
								.add("password", password)
								.build()
				)
		);

		return urls.stream().parallel()
				.flatMap(pattern -> generateFivePages(pattern))
				.map(connection::connectToAndGet)
				.flatMap(Optionals::asStream)
				.flatMap(ZalukajSelectors.TITLES::extractFrom)
				.map(this::parseToTitle);
	}

	Stream<String> generateFivePages(String pattern) {
		if (pattern.contains("%d")) {
			AtomicInteger i = new AtomicInteger(1);
			return Stream.generate(
					() -> String.format(pattern, i.incrementAndGet()))
					.limit(5);
		} else {
			return Stream.of(pattern);
		}
	}

	Title parseToTitle(String titleAsText) {
		Title.TitleBuilder builder = Title.builder();

		Matcher yearMatcher = YEAR.matcher(titleAsText);
		if (yearMatcher.matches()) {
			builder.year(
					Integer.valueOf(yearMatcher.group(1))
			);
		}

		Matcher titleMatcher = ORIGINAL_TITLE.matcher(titleAsText);
		if (titleMatcher.matches()) {
			builder.title(titleMatcher.group(1));
		}

		return builder.build();
	}

	@Override
	public String urlFor(Title title) {
		return null;
	}
}
