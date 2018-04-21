package pl.ciruk.core.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public final class Threads {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        return new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(threadNamePrefix + "-%s")
                .setUncaughtExceptionHandler((thread, throwable) -> LOGGER.error("Uncaught exception in {}", thread, throwable))
                .build();
    }
}
