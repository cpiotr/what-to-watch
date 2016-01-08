package pl.ciruk.whattowatch.config;

import com.squareup.okhttp.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.AllCookies;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.core.net.JsoupConnection;

@Configuration
public class Application {

	@Bean
	OkHttpClient httpClient() {
		OkHttpClient httpClient = new OkHttpClient();
		return httpClient;
	}

	@Bean
	@Qualifier("allCookies")
	<T> JsoupConnection jsoupConnectionAllCookies(CacheProvider<String> cacheProvider, OkHttpClient httpClient) {
		new AllCookies().applyTo(httpClient);
		return new JsoupCachedConnection(cacheProvider, httpClient);
	}

	@Bean
	<T> JsoupConnection jsoupConnection(CacheProvider<String> cacheProvider, OkHttpClient httpClient) {
		return new JsoupCachedConnection(cacheProvider, httpClient);
	}

}
