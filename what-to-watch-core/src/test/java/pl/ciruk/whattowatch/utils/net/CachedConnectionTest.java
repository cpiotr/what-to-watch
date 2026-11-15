package pl.ciruk.whattowatch.utils.net;

import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.utils.cache.CacheProvider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CachedConnectionTest {
    private static final HttpUrl URL = HttpUrl.get("https://www.example.com/film/");
    private static final String PAGE = "page";

    @SuppressWarnings("unchecked")
    private final HttpConnection<String> connection = mock(HttpConnection.class);
    private final InMemoryCache cache = new InMemoryCache();
    private final CachedConnection cachedConnection = new CachedConnection(cache, connection);

    @Test
    void shouldGetPageOnceAndThenFromCache() {
        when(connection.get(URL)).thenReturn(Optional.of(PAGE));

        assertThat(cachedConnection.get(URL)).contains(PAGE);
        assertThat(cachedConnection.get(URL)).contains(PAGE);

        verify(connection, times(1)).get(URL);
    }

    @Test
    void shouldLookForMissingPageOnce() {
        when(connection.get(URL)).thenReturn(Optional.empty());

        assertThat(cachedConnection.get(URL)).isEmpty();
        assertThat(cachedConnection.get(URL)).isEmpty();

        verify(connection, times(1)).get(URL);
    }

    @Test
    void shouldNotCacheFailure() {
        when(connection.get(URL))
                .thenThrow(new RequestException("Got 500 from " + URL))
                .thenReturn(Optional.of(PAGE));

        assertThatThrownBy(() -> cachedConnection.get(URL)).isInstanceOf(RequestException.class);
        assertThat(cachedConnection.get(URL)).contains(PAGE);
    }

    private static final class InMemoryCache implements CacheProvider<String> {
        private final Map<String, String> values = new ConcurrentHashMap<>();

        @Override
        public void put(String key, String value) {
            values.put(key, value);
        }

        @Override
        public Optional<String> get(String key) {
            return Optional.ofNullable(values.get(key));
        }
    }
}
