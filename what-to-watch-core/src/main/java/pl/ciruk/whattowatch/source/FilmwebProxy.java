package pl.ciruk.whattowatch.source;

import com.squareup.okhttp.HttpUrl;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

@Named
public class FilmwebProxy {
	private final HttpConnection<Element> connection;

	@Inject
	public FilmwebProxy(@Named("noCookiesHtml") HttpConnection<Element> connection) {
		this.connection = connection;
	}

	public Optional<Element> searchFor(String title, int year) {
		HttpUrl url = new HttpUrl.Builder()
				.scheme("http")
				.host("www.filmweb.pl")
				.addPathSegment("search/film")
				.addQueryParameter("q", title)
				.addQueryParameter("startYear", String.valueOf(year))
				.addQueryParameter("endYear", String.valueOf(year))
				.build();

		return Optional.ofNullable(url)
				.map(Object::toString)
				.flatMap(connection::connectToAndGet);
	}

	public Optional<Element> getPageWithFilmDetailsFor(String href) {
		HttpUrl url = new HttpUrl.Builder()
				.scheme("http")
				.host("www.filmweb.pl")
				.addPathSegment(href)
				.build();

		return Optional.ofNullable(url)
				.map(Object::toString)
				.flatMap(connection::connectToAndGet);
	}

}
