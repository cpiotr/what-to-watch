package pl.ciruk.whattowatch.utils.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public final class Threads {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Threads() {
        throw new AssertionError();
    }

    public static Thread.UncaughtExceptionHandler createUncaughtExceptionHandler() {
        return (thread, throwable) -> LOGGER.error("[{}] Uncaught exception", thread.getName(), throwable);
    }
}
