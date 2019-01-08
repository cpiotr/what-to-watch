package pl.ciruk.whattowatch.utils.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class Threads {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Threads() {
        throw new AssertionError();
    }

    public static void setThreadNamePrefix(String threadPrefix, ExecutorService pool) {
        try {
            Field workerNamePrefix = pool.getClass().getDeclaredField("workerNamePrefix");
            workerNamePrefix.setAccessible(true);
            workerNamePrefix.set(pool, threadPrefix + "-");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.warn("Could not change name prefix for thread pool", e);
        }
    }

    public static ThreadFactory createThreadFactory(String threadNamePrefix) {
        return new ThreadFactory() {
            private final AtomicLong counter = new AtomicLong();
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName(threadNamePrefix + "-" + counter.incrementAndGet());
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler((t, throwable) -> LOGGER.error("Uncaught exception in {}", t, throwable));
                return thread;
            }
        };
    }
}
