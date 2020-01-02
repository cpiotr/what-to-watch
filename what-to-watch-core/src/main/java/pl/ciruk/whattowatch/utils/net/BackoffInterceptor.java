package pl.ciruk.whattowatch.utils.net;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntConsumer;

public class BackoffInterceptor implements Interceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int BACKOFF_THRESHOLD = 1;

    private final Map<String, AtomicInteger> errorsByDomain = new ConcurrentHashMap<>();
    private final IntConsumer backOffFunction;
    private final int httpConnectionFixedDelayInterval;
    private final TimeUnit httpConnectionFixedDelayUnit;

    public BackoffInterceptor() {
        this(0, TimeUnit.MILLISECONDS, BackoffInterceptor::backOff);
    }

    public BackoffInterceptor(int httpConnectionFixedDelayInterval, TimeUnit httpConnectionFixedDelayUnit) {
        this(httpConnectionFixedDelayInterval, httpConnectionFixedDelayUnit, BackoffInterceptor::backOff);
    }

    BackoffInterceptor(int httpConnectionFixedDelayInterval, TimeUnit httpConnectionFixedDelayUnit, IntConsumer backOffFunction) {
        this.httpConnectionFixedDelayInterval = httpConnectionFixedDelayInterval;
        this.httpConnectionFixedDelayUnit = httpConnectionFixedDelayUnit;
        this.backOffFunction = backOffFunction;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (httpConnectionFixedDelayInterval > 0) {
            LockSupport.parkNanos(httpConnectionFixedDelayUnit.toNanos(httpConnectionFixedDelayInterval));
        }

        Request request = chain.request();
        String domain = request.url().topPrivateDomain();
        AtomicInteger errors = errorsByDomain.computeIfAbsent(domain, key -> new AtomicInteger());
        int errorsValue = errors.get();
        if (errorsValue >= BACKOFF_THRESHOLD) {
            LOGGER.info("Backing off of {}", domain);
            backOffFunction.accept(errorsValue);
        }
        try {
            Response response = chain.proceed(request);
            errors.set(0);
            return response;
        } catch (IOException e) {
            errors.incrementAndGet();
            throw e;
        }
    }

    private static void backOff(int errorsValue) {
        long waitTimeMillis = calculateTimeToWait(errorsValue);

        LOGGER.debug("Waiting {} ms", waitTimeMillis);
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitTimeMillis));
    }

    private static int calculateTimeToWait(int errorsValue) {
        int cappedNumberOfErrors = Math.min(errorsValue, 5);
        int exponentialOffset = 9; // Start with 512 ms

        return 1 << (exponentialOffset + cappedNumberOfErrors);
    }
}
