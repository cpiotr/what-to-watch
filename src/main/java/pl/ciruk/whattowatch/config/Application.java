package pl.ciruk.whattowatch.config;

import com.squareup.okhttp.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.core.net.JsoupConnection;

@Configuration
public class Application {
	@Bean
	<T> CacheProvider<T> provideCacheProvider() {
		return CacheProvider.empty();
	}

	@Bean
	OkHttpClient provideHttpClient() {
		return new OkHttpClient();
	}

	@Bean
	<T> JsoupConnection provideJsoupConnection(CacheProvider<T> cacheProvider, OkHttpClient httpClient) {
		return new JsoupCachedConnection(CacheProvider.<String>empty(), httpClient);
	}
}

