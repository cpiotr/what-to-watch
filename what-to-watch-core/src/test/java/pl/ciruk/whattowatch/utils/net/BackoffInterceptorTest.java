package pl.ciruk.whattowatch.utils.net;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.function.IntConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BackoffInterceptorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private IntConsumer backOffFunction;
    private BackoffInterceptor backoffInterceptor;

    @BeforeEach
    void setUp() {
        backOffFunction = mock(IntConsumer.class);
        backoffInterceptor = new BackoffInterceptor(backOffFunction);
    }

    @Test
    void shouldBackOffAfterTwoFailures() throws IOException {
        String invalidUrl = "http://thi-is.invalid.url";
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        Request request = new Request.Builder().url(invalidUrl).build();
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any())).thenThrow(new IOException());

        interceptIgnoringException(chain);
        interceptIgnoringException(chain);
        interceptIgnoringException(chain);

        verify(backOffFunction).accept(2);
    }

    @Test
    void shouldNotBackOffWhenFailedOnlyOnce() throws IOException {
        String invalidUrl = "http://thi-is.invalid.url";
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        Request request = new Request.Builder().url(invalidUrl).build();
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any())).thenThrow(new IOException());

        interceptIgnoringException(chain);
        interceptIgnoringException(chain);

        verifyZeroInteractions(backOffFunction);
    }

    @Test
    void shouldBackOffOnceWhenRequestStartsSucceeding() throws IOException {
        String invalidUrl = "http://thi-is.invalid.url";
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        Request request = new Request.Builder().url(invalidUrl).build();
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any())).thenThrow(new IOException());

        interceptIgnoringException(chain);
        interceptIgnoringException(chain);

        interceptIgnoringException(chain);

        doReturn(null).when(chain).proceed(any());
        interceptIgnoringException(chain);
        reset(backOffFunction);

        interceptIgnoringException(chain);

        verifyZeroInteractions(backOffFunction);
    }

    private Response interceptIgnoringException(Interceptor.Chain chain) {
        try {
            return backoffInterceptor.intercept(chain);
        } catch (IOException e) {
            LOGGER.debug("Ignoring {}", e.toString());
            LOGGER.trace("Caused by {}", e.getMessage(), e);
            return null;
        }
    }
}
