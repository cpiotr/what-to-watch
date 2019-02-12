package pl.ciruk.whattowatch.utils.net.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

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
    public Optional<JsonNode> connectToAndGet(HttpUrl url) {
        return connection.connectToAndGet(url)
                .flatMap(this::parseFrom);
    }

    @Override
    public Optional<JsonNode> connectToAndConsume(HttpUrl url, Consumer<Request.Builder> action) {
        return connection.connectToAndConsume(url, action)
                .flatMap(this::parseFrom);
    }

    private Optional<JsonNode> parseFrom(String text) {
        var objectMapper = new ObjectMapper();
        try {
            return Optional.ofNullable(
                    objectMapper.readTree(text));
        } catch (IOException e) {
            LOGGER.warn("Cannot parse json: {}", text, e);
            return Optional.empty();
        }
    }
}
