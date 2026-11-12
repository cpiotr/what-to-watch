package pl.ciruk.whattowatch.core.title.justwatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.title.TitleKind;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// Films or series that streaming services offer in Poland, most popular first, from the GraphQL API behind JustWatch's site
public class JustWatchTitleProvider implements TitleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpUrl API_URL = HttpUrl.get("https://apis.justwatch.com/graphql");
    private static final String BASE_URL = "https://www.justwatch.com";
    private static final MediaType JSON = MediaType.get("application/json");
    private static final int PAGE_SIZE = 100;
    private static final String QUERY = """
            query PopularTitles($after: String, $first: Int!, $packages: [String!], $objectTypes: [ObjectType!]) {
              popularTitles(
                  country: PL
                  first: $first
                  after: $after
                  sortBy: POPULAR
                  filter: {packages: $packages, objectTypes: $objectTypes}) {
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
    private final Counter numberOfTitles = Metrics.counter(
            Names.createName(TitleProvider.class, List.of("count")),
            List.of(Tags.getProviderTag("justwatch")));

    // The packages are JustWatch's short names of streaming services, such as nfx for Netflix
    public JustWatchTitleProvider(HttpConnection<String> connection, List<String> packages) {
        this.connection = connection;
        this.packages = List.copyOf(packages);

        LOGGER.info("Streaming services: {}", packages);
    }

    @Override
    public Stream<Title> streamOfTitles(TitleKind kind, int pageNumber) {
        LOGGER.debug("Kind: {}, page number: {}", kind, pageNumber);

        var body = RequestBody.create(createQueryFor(kind, pageNumber), JSON);
        return connection.post(API_URL, body)
                .map(JustWatchTitleProvider::titlesIn)
                .stream()
                .flatMap(titles -> StreamSupport.stream(titles.spliterator(), false))
                .map(edge -> parseToTitle(edge.path("node"), kind));
    }

    private String createQueryFor(TitleKind kind, int pageNumber) {
        var variables = Map.of(
                "after", cursorAfter((pageNumber - 1) * PAGE_SIZE),
                "first", PAGE_SIZE,
                "packages", packages,
                "objectTypes", List.of(objectTypeOf(kind)));
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of("query", QUERY, "variables", variables));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String objectTypeOf(TitleKind kind) {
        return switch (kind) {
            case FILM -> "MOVIE";
            case SERIES -> "SHOW";
        };
    }

    // A cursor is the number of titles before the page, in Base64
    private static String cursorAfter(int numberOfTitles) {
        return Base64.getEncoder().encodeToString(String.valueOf(numberOfTitles).getBytes(StandardCharsets.UTF_8));
    }

    // A broken answer throws, so it isn't shown as a page without titles
    private static JsonNode titlesIn(String json) {
        JsonNode response;
        try {
            response = OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JustWatch answered with something other than JSON", e);
        }
        var titles = response.path("data").path("popularTitles").path("edges");
        if (!titles.isArray()) {
            throw new IllegalStateException("JustWatch answered without titles: " + response.path("errors"));
        }
        if (response.has("errors")) {
            LOGGER.warn("JustWatch answered with errors: {}", response.path("errors"));
        }
        return titles;
    }

    // Links to the title on a streaming service, or to JustWatch's page of it when no service offers it anymore
    private Title parseToTitle(JsonNode node, TitleKind kind) {
        numberOfTitles.increment();
        var content = node.path("content");
        var url = StreamSupport.stream(node.path("offers").spliterator(), false)
                .map(offer -> offer.path("standardWebURL").asText())
                .filter(link -> !link.isEmpty())
                .findFirst()
                .orElse(BASE_URL + content.path("fullPath").asText());

        return Title.builder()
                .title(content.path("title").asText())
                .originalTitle(content.path("originalTitle").asText())
                .year(content.path("originalReleaseYear").asInt(Title.MISSING_YEAR))
                .url(url)
                .kind(kind)
                .build();
    }
}
