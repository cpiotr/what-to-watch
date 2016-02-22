package pl.ciruk.core.net.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.Request;
import lombok.extern.slf4j.Slf4j;
import pl.ciruk.core.net.HttpConnection;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class JsonConnection implements HttpConnection<JsonNode> {

	private final HttpConnection<String> connection;

	public JsonConnection(HttpConnection<String> connection) {
		this.connection = connection;
	}

	@Override
	public Optional<JsonNode> connectToAndGet(String url) {
		return connection.connectToAndGet(url)
				.flatMap(this::parseFrom);
	}

	@Override
	public Optional<JsonNode> connectToAndConsume(String url, Consumer<Request.Builder> action) {
		return connection.connectToAndConsume(url, action)
				.flatMap(this::parseFrom);
	}

	private Optional<JsonNode> parseFrom(String text) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return Optional.ofNullable(
					objectMapper.readTree(text));
		} catch (IOException e) {
			log.warn("parseFrom - Cannot parse json: {}", text, e);
			return Optional.empty();
		}
	}
}
