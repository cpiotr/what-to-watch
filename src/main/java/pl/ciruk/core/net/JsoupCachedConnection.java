package pl.ciruk.core.net;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import pl.ciruk.core.cache.CacheProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Named
@Slf4j
public class JsoupCachedConnection implements JsoupConnection {

	private CacheProvider<String> cache;

	private Set<String> cookies;

	private OkHttpClient httpClient;

	@Inject
	public JsoupCachedConnection(CacheProvider<String> cache, OkHttpClient httpClient) {
		this.cache = cache;
		this.httpClient = httpClient;
		cookies = new HashSet<>();
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
				Response response = execute(to(url));
				content = Jsoup.parse(response.body().string());
				cache.put(url, content.html());
			} catch (IOException e) {
				log.warn("connectToAndGet - Cannot fetch " + url, e);
			}
			return Optional.ofNullable(content);
		}
	}

	private Response execute(Request.Builder requestBuilder) throws IOException {
		cookies.stream()
				.forEach(cookie -> requestBuilder.addHeader("Cookie", cookie));
		Request build = requestBuilder.build();
		Response response = httpClient.newCall(build).execute();
		cookies.addAll(
				response.headers("Set-Cookie")

		);
		return response;
	}

	public Optional<Element> connectToAndConsume(String url, Consumer<Request.Builder> action) {
		Request.Builder builder = to(url);

		action.accept(builder);
		try {
			Response response = execute(builder);
			return Optional.ofNullable(Jsoup.parse(response.body().string()));
		} catch (IOException e) {
			throw new RuntimeException("Cannot process request to " + url, e);
		}
	}

	Request.Builder to(String url) {
		return new Request.Builder()
				.url(url)
				.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
//				.header("User-Agent", "Mozilla/5.0 (compatible, MSIE 11, Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko")
				.addHeader("Accept-Language", "pl")
				.addHeader("Referer", rootDomainFor(url));
	}

	private static String rootDomainFor(String url) {
		URI uri = URI.create(url);
		String port = uri.getPort() > -1
				? ":" + uri.getPort()
				: "";

		return String.format("%s://%s%s/", uri.getScheme(), uri.getHost(), port);
	}
}
