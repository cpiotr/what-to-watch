package pl.ciruk.whattowatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.core.cache.CacheProvider;
import pl.ciruk.core.net.JsoupCachedConnection;
import pl.ciruk.core.net.JsoupConnection;

@Configuration
public class Application {
	@Bean
	JsoupConnection provideJsoupConnection() {
		return new JsoupCachedConnection(CacheProvider.<String>empty());
	}
}

