package pl.ciruk.core.net;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import pl.ciruk.core.cache.CacheProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Named
@Slf4j
public class JsoupCachedConnection implements JsoupConnection {

	private CacheProvider<String> cache;

	private Map<String, String> cookies;

	@Inject
	public JsoupCachedConnection(CacheProvider<String> cache) {
		this.cache = cache;
		cookies = Maps.newHashMap();
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
				Connection.Response response = to(url).cookies(cookies).execute();
				cookies.putAll(response.cookies());
				content = response.parse();
				cache.put(url, content.html());
			} catch (IOException e) {
				log.warn("connectToAndGet - Cannot fetch " + url, e);
			}
			return Optional.ofNullable(content);
		}
	}

	public Optional<Element> connectToAndConsume(String url, Consumer<Connection> action) {
		Connection connection = to(url);
		action.accept(connection);
		try {
			Connection.Response response = connection.followRedirects(true).execute();
			cookies.putAll(response.cookies());
			return Optional.ofNullable(response.parse());
		} catch (IOException e) {
			throw new RuntimeException("Cannot process request to " + url, e);
		}
	}

	Connection to(String url) {
		return Jsoup.connect(url)
				.timeout(60 * 1000)
				.userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
				.userAgent("Mozilla/5.0 (compatible, MSIE 11, Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko")
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
