package pl.ciruk.core.net;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.redis.core.StringRedisTemplate;

public class JsoupConnection {

	private StringRedisTemplate cache;

	public JsoupConnection() {
	}

	public JsoupConnection(StringRedisTemplate cache) {
		this.cache = cache;
	}

	public Optional<Element> connectToAndGet(String url) {
		Optional<Element> document = Optional.ofNullable(cache.opsForValue().get(url))
				.map(Jsoup::parse);
		if (document.isPresent()) {
			return document;
		} else {
			Element content = null;
			try {
				content = to(url).get();
				cache.opsForValue().set(url, content.html());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return Optional.ofNullable(content);
		}
	}

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
