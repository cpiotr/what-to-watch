package pl.ciruk.core.net;

import java.net.URI;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class JsoupConnection {
	public static Connection connectTo(String url) {
		return Jsoup.connect(url)
				.timeout(60 * 1000)
				.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0")
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
