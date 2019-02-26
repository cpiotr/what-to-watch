package pl.ciruk.whattowatch.utils.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

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
                thread.setUncaughtExceptionHandler(createUncaughtExceptionHandler());
                return thread;
            }
        };
    }

    public static Thread.UncaughtExceptionHandler createUncaughtExceptionHandler() {
        return (thread, throwable) -> LOGGER.error("[{}] Uncaught exception", thread.getName(), throwable);
    }

    public static <T> T manageBlocking(Supplier<T> supplier) {
        SupplierManagedBlock<T> managedBlock = new SupplierManagedBlock<>(supplier);
        try {
            ForkJoinPool.managedBlock(managedBlock);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return managedBlock.getResult();
    }

    private static class SupplierManagedBlock<T> implements ForkJoinPool.ManagedBlocker {
        private final Supplier<T> supplier;
        private T result;
        private boolean finished;

        private SupplierManagedBlock(final Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public boolean block() {
            result = supplier.get();
            finished = true;
            return true;
        }

        @Override
        public boolean isReleasable() {
            return finished;
        }

        public T getResult() {
            return result;
        }
    }
}
