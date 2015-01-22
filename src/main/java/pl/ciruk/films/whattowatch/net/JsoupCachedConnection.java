package pl.ciruk.films.whattowatch.net;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import pl.ciruk.core.cache.CacheProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Named
public class JsoupCachedConnection implements JsoupConnection {

	private CacheProvider<String> cache;

	@Inject
	public JsoupCachedConnection(CacheProvider<String> cache) {
		this.cache = cache;
	}

	@Override
	public Optional<Element> connectToAndGet(String url) {
		Optional<Element> document = cache.get(url)
				.map(Jsoup::parse);

		if (document.isPresent()) {
			return document;
		} else {
			Element content = null;
			try {
				content = to(url).get();
				cache.put(url, content.html());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return Optional.ofNullable(content);
		}
	}

	@Override
	public Connection to(String url) {
		return Jsoup.connect(url)
				.timeout(60 * 1000)
				.userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
				.header("Accept-Language", "pl")
				.referrer(rootDomainFor(url));
	}

	private static String rootDomainFor(String url) {
		URI uri = URI.create(url);
		String port = uri.getPort() > -1
				? ":" + uri.getPort()
				: "";

		return String.format("%s://%s%s/", uri.getScheme(), uri.getHost(), port);
	}
}
