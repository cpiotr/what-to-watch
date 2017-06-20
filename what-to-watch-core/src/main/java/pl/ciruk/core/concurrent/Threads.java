package pl.ciruk.core.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

@Slf4j
public class Threads {
    public static void setThreadNamePrefix(String threadPrefix, ExecutorService pool) {
        try {
            Field workerNamePrefix = pool.getClass().getDeclaredField("workerNamePrefix");
            workerNamePrefix.setAccessible(true);
            workerNamePrefix.set(pool, threadPrefix);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Could not change name prefix for thread pool", e);
        }
    }
}
