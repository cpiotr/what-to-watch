package pl.ciruk.whattowatch.core.title.justwatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// Films that streaming services offer in Poland, most popular first, from the GraphQL API behind JustWatch's site
public class JustWatchTitleProvider implements TitleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpUrl API_URL = HttpUrl.get("https://apis.justwatch.com/graphql");
    private static final String BASE_URL = "https://www.justwatch.com";
    private static final MediaType JSON = MediaType.get("application/json");
    private static final int PAGE_SIZE = 100;
    private static final String QUERY = """
            query PopularTitles($after: String, $first: Int!, $packages: [String!]) {
              popularTitles(
                  country: PL
                  first: $first
                  after: $after
                  sortBy: POPULAR
                  filter: {packages: $packages, objectTypes: [MOVIE]}) {
                edges {
                  node {
                    content(country: PL, language: "pl") {
                      title
                      originalTitle
                      originalReleaseYear
                      fullPath
                    }
                    offers(country: PL, platform: WEB, filter: {packages: $packages, monetizationTypes: [FLATRATE]}) {
                      standardWebURL
                    }
                  }
                }
              }
            }""";

    private final HttpConnection<String> connection;
    private final List<String> packages;
    private final AtomicLong numberOfTitles = new AtomicLong();

    // The packages are JustWatch's short names of streaming services, such as nfx for Netflix
    public JustWatchTitleProvider(HttpConnection<String> connection, List<String> packages) {
        this.connection = connection;
        this.packages = List.copyOf(packages);

        Metrics.gauge(
                Names.createName(TitleProvider.class, List.of("count")),
                List.of(Tags.getProviderTag("justwatch")),
                numberOfTitles,
                AtomicLong::get);

        LOGGER.info("Streaming services: {}", packages);
    }

    @Override
    public Stream<Title> streamOfTitles(int pageNumber) {
        LOGGER.debug("Page number: {}", pageNumber);

        var body = RequestBody.create(createQueryFor(pageNumber), JSON);
        return connection.connectToAndConsume(API_URL, request -> request.post(body))
                .flatMap(JustWatchTitleProvider::readJson)
                .stream()
                .flatMap(response -> StreamSupport.stream(response.path("data").path("popularTitles").path("edges").spliterator(), false))
                .map(edge -> parseToTitle(edge.path("node")))
                .peek(ignored -> numberOfTitles.incrementAndGet());
    }

    private String createQueryFor(int pageNumber) {
        var variables = Map.of(
                "after", cursorAfter((pageNumber - 1) * PAGE_SIZE),
                "first", PAGE_SIZE,
                "packages", packages);
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of("query", QUERY, "variables", variables));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    // A cursor is the number of titles before the page, in Base64
    private static String cursorAfter(int numberOfTitles) {
        return Base64.getEncoder().encodeToString(String.valueOf(numberOfTitles).getBytes(StandardCharsets.UTF_8));
    }

    private static Optional<JsonNode> readJson(String json) {
        try {
            var response = OBJECT_MAPPER.readTree(json);
            if (response.has("errors")) {
                LOGGER.warn("JustWatch answered with errors: {}", response.path("errors"));
            }
            return Optional.of(response);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Could not parse JSON from {}", API_URL, e);
            return Optional.empty();
        }
    }

    // Links to the film on a streaming service, or to JustWatch's page of it when no service offers it anymore
    private static Title parseToTitle(JsonNode film) {
        var content = film.path("content");
        var url = StreamSupport.stream(film.path("offers").spliterator(), false)
                .map(offer -> offer.path("standardWebURL").asText())
                .filter(link -> !link.isEmpty())
                .findFirst()
                .orElse(BASE_URL + content.path("fullPath").asText());

        return Title.builder()
                .title(content.path("title").asText())
                .originalTitle(content.path("originalTitle").asText())
                .year(content.path("originalReleaseYear").asInt(Title.MISSING_YEAR))
                .url(url)
                .build();
    }
}
