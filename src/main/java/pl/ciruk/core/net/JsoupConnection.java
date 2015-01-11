package pl.ciruk.core.net;

import java.net.URI;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class JsoupConnection {
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
