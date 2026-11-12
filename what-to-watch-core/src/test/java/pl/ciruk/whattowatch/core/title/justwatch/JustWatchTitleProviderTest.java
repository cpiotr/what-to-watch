package pl.ciruk.whattowatch.core.title.justwatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.net.HttpConnection;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JustWatchTitleProviderTest {
    private static final HttpUrl API_URL = HttpUrl.get("https://apis.justwatch.com/graphql");

    private HttpConnection<String> connection;
    private JustWatchTitleProvider provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(HttpConnection.class);
        provider = new JustWatchTitleProvider(connection, List.of("nfx", "prv"));
    }

    @Test
    void shouldLinkToStreamingServiceOfferingFilm() {
        givenResponse("""
                {"data":{"popularTitles":{"edges":[{"node":{
                  "content":{"title":"Projekt Hail Mary","originalTitle":"Project Hail Mary","originalReleaseYear":2026,"fullPath":"/pl/film/projekt-hail-mary"},
                  "offers":[{"standardWebURL":"https://app.primevideo.com/detail?gti=1"},{"standardWebURL":"https://app.primevideo.com/detail?gti=1"}]}}]}}}""");

        var titles = provider.streamOfTitles(TitleKind.FILM, 1).toList();

        assertThat(titles).containsExactly(
                new Title("Project Hail Mary", "Projekt Hail Mary", 2026, "https://app.primevideo.com/detail?gti=1", TitleKind.FILM));
    }

    @Test
    void shouldLinkToJustWatchWhenNoServiceOffersFilm() {
        givenResponse("""
                {"data":{"popularTitles":{"edges":[{"node":{
                  "content":{"title":"Doktor Sen","originalTitle":"Doctor Sleep","originalReleaseYear":2019,"fullPath":"/pl/film/doktor-sen"},
                  "offers":[]}}]}}}""");

        var titles = provider.streamOfTitles(TitleKind.FILM, 1).toList();

        assertThat(titles).extracting(Title::url).containsExactly("https://www.justwatch.com/pl/film/doktor-sen");
    }

    @Test
    void shouldAskForSeriesAndKeepTheirKind() throws IOException {
        givenResponse("""
                {"data":{"popularTitles":{"edges":[{"node":{
                  "content":{"title":"Below","originalTitle":"Below","originalReleaseYear":2026,"fullPath":"/pl/serial/below-2026"},
                  "offers":[]}}]}}}""");

        var titles = provider.streamOfTitles(TitleKind.SERIES, 1).toList();

        assertThat(titles).extracting(Title::kind).containsExactly(TitleKind.SERIES);
        assertThat(sentRequestBody().path("variables").path("objectTypes")).extracting(JsonNode::asText).containsExactly("SHOW");
    }

    @Test
    void shouldFailWhenResponseHasErrorsInsteadOfTitles() {
        givenResponse("""
                {"errors":[{"message":"transport not supported"}],"data":null}""");

        assertThatThrownBy(() -> provider.streamOfTitles(TitleKind.FILM, 1).toList())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transport not supported");
    }

    @Test
    void shouldFailWhenResponseIsNotJson() {
        givenResponse("<html>Service unavailable</html>");

        assertThatThrownBy(() -> provider.streamOfTitles(TitleKind.FILM, 1).toList()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAskForPageAfterTitlesOfPreviousPages() throws IOException {
        givenResponse("""
                {"data":{"popularTitles":{"edges":[]}}}""");

        assertThat(provider.streamOfTitles(TitleKind.FILM, 3)).isEmpty();

        var variables = sentRequestBody().path("variables");
        assertThat(variables.path("after").asText()).isEqualTo("MjAw");
        assertThat(variables.path("first").asInt()).isEqualTo(100);
        assertThat(variables.path("packages")).extracting(JsonNode::asText).containsExactly("nfx", "prv");
        assertThat(variables.path("objectTypes")).extracting(JsonNode::asText).containsExactly("MOVIE");
    }

    @Test
    void shouldStartFirstPageAtFirstTitle() throws IOException {
        givenResponse("""
                {"data":{"popularTitles":{"edges":[]}}}""");

        assertThat(provider.streamOfTitles(TitleKind.FILM, 1)).isEmpty();

        assertThat(sentRequestBody().path("variables").path("after").asText()).isEqualTo("MA==");
    }

    private void givenResponse(String json) {
        when(connection.post(eq(API_URL), any())).thenReturn(Optional.of(json));
    }

    private JsonNode sentRequestBody() throws IOException {
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(connection).post(eq(API_URL), body.capture());

        try (var buffer = new Buffer()) {
            body.getValue().writeTo(buffer);
            return new ObjectMapper().readTree(buffer.readUtf8());
        }
    }
}
