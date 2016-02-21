package pl.ciruk.core.net.html;

import com.squareup.okhttp.Request;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import pl.ciruk.core.net.HttpConnection;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class JsoupConnection implements HttpConnection<Element> {

	private HttpConnection<String> connection;

	@Inject
	public JsoupConnection(HttpConnection<String> connection) {
		this.connection = connection;
	}

	@PostConstruct
	public void init() {
		log.debug("init - Connection: {}", connection);
	}

	@Override
	public Optional<Element> connectToAndGet(String url) {
		return connection.connectToAndGet(url)
				.map(Jsoup::parse);
	}

	public Optional<Element> connectToAndConsume(String url, Consumer<Request.Builder> action) {
		return connectToAndConsume(url, action);
	}
}
