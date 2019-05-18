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
    private static final int BACKOFF_THRESHOLD = 2;

    private final Map<String, AtomicInteger> errorsByDomain = new ConcurrentHashMap<>();
    private final IntConsumer backOffFunction;

    public BackoffInterceptor() {
        this(BackoffInterceptor::backOff);
    }

    BackoffInterceptor(IntConsumer backOffFunction) {
        this.backOffFunction = backOffFunction;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
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
        double exponential = Math.pow(2, errorsValue % 6);
        long waitTimeMillis = (long) (exponential * 100);
        LOGGER.debug("Waiting {} ms", waitTimeMillis);
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitTimeMillis));
    }
}
