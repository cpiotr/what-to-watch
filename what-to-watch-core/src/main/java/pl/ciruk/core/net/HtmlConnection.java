package pl.ciruk.core.net;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class HtmlConnection implements HttpConnection<String> {
	private final OkHttpClient httpClient;

	public HtmlConnection(OkHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@PostConstruct
	public void init() {
		log.debug("init: HttpClient: {}", httpClient);
		httpClient.interceptors().add(this::log);
	}

	@Override
	public Optional<String> connectToAndGet(String url) {
		log.debug("connectToAndGet- Url: {}", url);

		try {
			Response response = execute(to(url));
			return Optional.ofNullable(
					response.body().string()
			);
		} catch (IOException e) {
			log.warn("connectToAndGet - Could no get {}", url, e);
			return Optional.empty();
		}
	}

	@Override
	public Optional<String> connectToAndConsume(String url, Consumer<Request.Builder> action) {
		log.debug("connectToAndConsume - Url: {}", url);
		Request.Builder builder = to(url);

		action.accept(builder);
		try {
			Response response = execute(builder);
			log.debug("Headers: {}", response.headers());
			String body = response.body().string();
			log.trace("Body: {}", body);
			return Optional.ofNullable(body);
		} catch (IOException e) {
			log.warn("Cannot process request to {}", url, e);
			return Optional.empty();
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
				.addHeader("User-Agent", UserAgents.next())
				.addHeader("Accept-Language", "pl")
				.addHeader("Referrer", rootDomainFor(url));
	}

	private Response log(Interceptor.Chain chain) throws IOException {
		Request request = chain.request();
		log.debug("Request: {}", request);
		Response response = chain.proceed(request);
		log.debug("Response: {}", response);
		return response;
	}

	private static String rootDomainFor(String url) {
		URI uri = URI.create(url);
		String port = uri.getPort() > -1
				? ":" + uri.getPort()
				: "";

		return String.format("%s://%s%s/", uri.getScheme(), uri.getHost(), port);
	}
}
