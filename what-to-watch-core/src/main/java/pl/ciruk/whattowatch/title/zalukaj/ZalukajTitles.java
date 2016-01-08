package pl.ciruk.whattowatch.title.zalukaj;

import com.google.common.collect.Lists;
import com.squareup.okhttp.FormEncodingBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.core.stream.Optionals;
import pl.ciruk.whattowatch.title.Title;
import pl.ciruk.whattowatch.title.TitleProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Named
@Slf4j
public class ZalukajTitles implements TitleProvider {
	public static final Pattern YEAR = Pattern.compile(".*\\(([12][0-9]{3})\\)$");
	public static final Pattern ORIGINAL_TITLE = Pattern.compile("(.*)\\((.*)\\)$");
	private JsoupCachedConnection connection;

	@Inject
	public ZalukajTitles(@Named("allCookies") JsoupCachedConnection connection) {
		this.connection = connection;
	}


	public ZalukajTitles(@Named("allCookies") JsoupCachedConnection connection, String login, String password) {
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
	public Stream<CompletableFuture<Stream<Title>>> streamOfTitles() {
		log.info("streamOfTitles");
		connection.connectToAndConsume(
				loginPage,
				request -> request.post(
						new FormEncodingBuilder()
								.add("login", login)
								.add("password", password)
								.build()
				)
		);

		return urls.stream()
				.flatMap(pattern -> generateFivePages(pattern))
				.map(url -> CompletableFuture.supplyAsync(() -> connection.connectToAndGet(url))
								.thenApply(Optionals::asStream)
								.thenApply(this::extractAndMapToTitles)
				);
	}

	private Stream<Title> extractAndMapToTitles(Stream<Element> element) {
		return element.flatMap(ZalukajSelectors.TITLES::extractFrom)
				.map(this::parseToTitle);
	}

	Stream<String> generateFivePages(String pattern) {
		if (pattern.contains("%d")) {
			AtomicInteger i = new AtomicInteger(1);
			return Stream.generate(
					() -> String.format(pattern, i.incrementAndGet()))
					.limit(1);
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
