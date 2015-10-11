package pl.ciruk.core.net;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import pl.ciruk.core.cache.CacheProvider;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
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
		cookies = new ConcurrentSkipListSet<>();
	}

	@PostConstruct
	public void init() {
		log.info("init");
		httpClient.interceptors().add(this::handleCookies);
		httpClient.interceptors().add(this::log);
		acceptAllCookies();
	}

	@Override
	public Optional<Element> connectToAndGet(String url) {
		log.debug("connectToAndGet- Url: {}", url);
		log.debug("connectToAndGet - Cookies: {}", cookies);

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

	public Optional<Element> connectToAndConsume(String url, Consumer<Request.Builder> action) {
		log.debug("connectToAndConsume - Url: {}", url);
		Request.Builder builder = to(url);

		action.accept(builder);
		try {
			Response response = execute(builder);
			return Optional.ofNullable(response.body().string())
					.map(Jsoup::parse);
		} catch (IOException e) {
			throw new RuntimeException("Cannot process request to " + url, e);
		}
	}

	private Response execute(Request.Builder requestBuilder) throws IOException {
		Request build = requestBuilder.build();
		Response response = httpClient.newCall(build).execute();
		return response;
	}

	Request.Builder to(String url) {
		return new Request.Builder()
				.url(url)
				.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
				.addHeader("Accept-Language", "pl")
				.addHeader("Referer", rootDomainFor(url));
	}

	private Response log(Interceptor.Chain chain) throws IOException {
		Request request = chain.request();
		log.debug("Request: {}", request);
		Response response = chain.proceed(request);
		log.debug("Response: {}", response);
		return response;
	}

	private void acceptAllCookies() {
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		httpClient.setCookieHandler(cookieManager);
	}

	private Response handleCookies(Interceptor.Chain chain) throws IOException {
		Request.Builder request = chain.request().newBuilder();
		attachCookiesTo(request);

		Response response = chain.proceed(request.build());
		readCookiesFrom(response);
		return response;
	}

	private void readCookiesFrom(Response response) {
		List<String> cookiesFromResponse = response.headers("Set-Cookie");
		cookies.addAll(cookiesFromResponse);
		log.debug("HTTP interceptor - Received cookies: {}", cookiesFromResponse);
	}

	private void attachCookiesTo(Request.Builder request) {
		cookies.stream()
				.forEach(cookie -> request.addHeader("Cookie", cookie));
	}

	private static String rootDomainFor(String url) {
		URI uri = URI.create(url);
		String port = uri.getPort() > -1
				? ":" + uri.getPort()
				: "";

		return String.format("%s://%s%s/", uri.getScheme(), uri.getHost(), port);
	}
}
