package pl.ciruk.whattowatch.title.zalukaj;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.squareup.okhttp.FormEncodingBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;
import pl.ciruk.core.stream.Optionals;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Named
@Slf4j
public class ZalukajTitles implements TitleProvider {
	public static final Pattern YEAR = Pattern.compile(".*\\(([12][0-9]{3})\\)$");
	public static final Pattern ORIGINAL_TITLE = Pattern.compile("(.*)\\((.*)\\)$");

	private final HttpConnection<Element> connection;

	@Inject
	public ZalukajTitles(@Named("allCookiesHtml") HttpConnection<Element> connection) {
		this.connection = connection;
	}


	public ZalukajTitles(@Named("allCookiesHtml") HttpConnection<Element> connection, String login, String password) {
		this.connection = connection;
		this.login = login;
		this.password = password;
	}

	String loginPage = "http://zalukaj.tv/account.php";

	String login;

	String password;

	List<String> urls = Lists.newArrayList(
			"http://zalukaj.tv/gatunek,14/ostatnio-dodane,wszystkie,strona-%d",
			"http://zalukaj.tv/gatunek,13/ostatnio-dodane,wszystkie,strona-%d",
			"http://zalukaj.tv/gatunek,10/ostatnio-dodane,wszystkie,strona-%d",
			"http://zalukaj.tv/gatunek,20/ostatnio-dodane,wszystkie,strona-%d",
			"http://zalukaj.tv/gatunek,22/ostatnio-dodane,wszystkie,strona-%d",
			"http://zalukaj.tv/gatunek,8/ostatnio-dodane,wszystkie,strona-%d",
			"http://zalukaj.tv/gatunek,4/ostatnio-dodane,wszystkie,strona-%d"
	);

	@PostConstruct
	void init() {
		log.debug("init - Login URL: {}. Credentials: {}", loginPage, login);
	}

	@Override
	public Stream<Title> streamOfTitles() {
		log.info("streamOfTitles");
		if (areCredentialsPresent()) {
			authenticate();
		}

		return urls.stream()
				.parallel()
				.flatMap(pattern -> generateFivePages(pattern))
				.map(connection::connectToAndGet)
				.flatMap(Optionals::asStream)
				.flatMap(ZalukajStreamSelectors.TITLE_LINKS::extractFrom)
				.map(this::parseToTitle);
	}

	private boolean areCredentialsPresent() {
		return !Strings.isNullOrEmpty(login)
				&& !Strings.isNullOrEmpty(password);
	}

	private void authenticate() {
		connection.connectToAndConsume(
				loginPage,
				request -> request.post(
						new FormEncodingBuilder()
								.add("login", login)
								.add("password", password)
								.build()
				)
		);
	}

	Stream<String> generateFivePages(String pattern) {
		if (pattern.contains("%d")) {
			AtomicInteger i = new AtomicInteger(1);
			return Stream.generate(
					() -> String.format(pattern, i.incrementAndGet()))
					.limit(2);
		} else {
			return Stream.of(pattern);
		}
	}

	Title parseToTitle(Element linkToTitle) {
		String titleAsText = ZalukajSelectors.TITLE.extractFrom(linkToTitle).orElse("");
		Title.TitleBuilder builder = Title.builder();
		builder.url(ZalukajSelectors.HREF.extractFrom(linkToTitle).orElse(""));

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
}
