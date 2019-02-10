package pl.ciruk.whattowatch.utils.net;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.function.IntConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HtmlConnectionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private HtmlConnection connection;
    private IntConsumer backOffFunction;
    private Call call;

    @BeforeEach
    void setUp() throws IOException {
        backOffFunction = mock(IntConsumer.class);

        call = mock(Call.class);
        doThrow(IOException.class).when(call).execute();

        OkHttpClient httpClient = mock(OkHttpClient.class);
        when(httpClient.newCall(any())).thenReturn(call);

        connection = new HtmlConnection(httpClient, backOffFunction);
    }

    @Test
    void shouldBackOffAfterTwoFailures() {
        String invalidUrl = "http://thi-is.invalid.url";
        connectToAndGet(invalidUrl);
        connectToAndGet(invalidUrl);

        connectToAndGet(invalidUrl);

        verify(backOffFunction).accept(2);
    }

    @Test
    void shouldNotBackOffWhenFailedOnlyOnce() {
        String invalidUrl = "http://thi-is.invalid.url";
        connectToAndGet(invalidUrl);

        connectToAndGet(invalidUrl);

        verifyZeroInteractions(backOffFunction);
    }

    @Test
    void shouldBackOffOnceWhenRequestStartsSucceeding() throws IOException {
        String invalidUrl = "http://thi-is.invalid.url";
        connectToAndGet(invalidUrl);
        connectToAndGet(invalidUrl);

        doReturn(null).when(call).execute();
        connectToAndGet(invalidUrl);
        reset(backOffFunction);

        connectToAndGet(invalidUrl);

        verifyZeroInteractions(backOffFunction);
    }

    private void connectToAndGet(String invalidUrl) {
        try {
            connection.connectToAndGet(invalidUrl);
        } catch (Exception e) {
            LOGGER.debug("Expected", e);
        }
    }
}
