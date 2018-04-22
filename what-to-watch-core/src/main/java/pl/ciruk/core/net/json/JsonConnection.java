package pl.ciruk.core.net.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.core.net.HttpConnection;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.function.Consumer;

public class JsonConnection implements HttpConnection<JsonNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        var objectMapper = new ObjectMapper();
        try {
            return Optional.ofNullable(
                    objectMapper.readTree(text));
        } catch (IOException e) {
            LOGGER.warn("parseFrom - Cannot parse json: {}", text, e);
            return Optional.empty();
        }
    }
}
