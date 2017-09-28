package pl.ciruk.core.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class Threads {
    public static void setThreadNamePrefix(String threadPrefix, ExecutorService pool) {
        try {
            Field workerNamePrefix = pool.getClass().getDeclaredField("workerNamePrefix");
            workerNamePrefix.setAccessible(true);
            workerNamePrefix.set(pool, threadPrefix + "-");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Could not change name prefix for thread pool", e);
        }
    }

    public static ThreadFactory createThreadFactory(String threadNamePrefix) {
        return new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(threadNamePrefix + "-%s")
                .setUncaughtExceptionHandler((thread, throwable) -> log.error("Uncaught exception in {}", thread, throwable))
                .build();
    }
}
