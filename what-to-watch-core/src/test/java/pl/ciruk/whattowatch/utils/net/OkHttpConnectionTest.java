package pl.ciruk.whattowatch.utils.net;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OkHttpConnectionTest {
    private static final String PAGE = "page";
    private static final String FILM_PATH = "/film";

    private final MockWebServer server = new MockWebServer();
    private final DomainRateLimiter rateLimiter = mock(DomainRateLimiter.class);
    private final OkHttpConnection connection = new OkHttpConnection(
            new OkHttpClient.Builder().readTimeout(Duration.ofMillis(200)).build(),
            rateLimiter);

    @BeforeEach
    void setUp() throws IOException {
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldReturnBodyOfSuccessfulResponse() {
        server.enqueue(new MockResponse().setBody(PAGE));

        assertThat(connection.get(server.url(FILM_PATH))).contains(PAGE);
    }

    @Test
    void shouldReturnNothingForMissingPage() {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("Not found"));

        assertThat(connection.get(server.url(FILM_PATH))).isEmpty();
    }

    @Test
    void shouldFailOnEmptyPage() {
        server.enqueue(new MockResponse().setResponseCode(202));
        var url = server.url(FILM_PATH);

        assertThatThrownBy(() -> connection.get(url)).isInstanceOf(RequestException.class);
    }

    @Test
    void shouldFailOnErrorPage() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal error"));
        var url = server.url(FILM_PATH);

        assertThatThrownBy(() -> connection.get(url)).isInstanceOf(RequestException.class);
    }

    @Test
    void shouldFailOnThrottledResponseAndReportIt() {
        server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "1").setBody("Slow down"));
        var url = server.url(FILM_PATH);

        assertThatThrownBy(() -> connection.get(url)).isInstanceOf(RequestException.class);
        verify(rateLimiter).recordResponse(eq(url), argThat((Response response) -> response.code() == 429));
    }

    @Test
    void shouldRetryOnceAfterTimeout() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        server.enqueue(new MockResponse().setBody(PAGE));
        var url = server.url(FILM_PATH);

        assertThat(connection.get(url)).contains(PAGE);
        verify(rateLimiter).recordFailure(url);
    }

    @Test
    void shouldGiveUpAfterSecondTimeout() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        server.enqueue(new MockResponse().setBody(PAGE));
        var url = server.url(FILM_PATH);

        assertThatThrownBy(() -> connection.get(url)).isInstanceOf(RequestException.class);
        assertThat(server.getRequestCount()).isEqualTo(2);
        verify(rateLimiter, times(2)).recordFailure(url);
    }

    @Test
    void shouldTakeTurnForRedirect() {
        server.enqueue(new MockResponse().setResponseCode(301).setHeader("Location", "/moved"));
        server.enqueue(new MockResponse().setBody(PAGE));
        var movedUrl = server.url("/moved");

        assertThat(connection.get(server.url(FILM_PATH))).contains(PAGE);
        verify(rateLimiter).recordRedirect(movedUrl);
        verify(rateLimiter).recordResponse(eq(movedUrl), any());
    }

    @Test
    void shouldPostBody() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("{}"));

        var answer = connection.post(server.url("/graphql"), RequestBody.create("{\"query\":1}", MediaType.get("application/json")));

        assertThat(answer).contains("{}");
        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"query\":1}");
    }
}
