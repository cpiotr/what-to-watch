package pl.ciruk.whattowatch.utils.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class Threads {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Threads() {
        throw new AssertionError();
    }

    public static ForkJoinPool.ForkJoinWorkerThreadFactory createForkJoinThreadFactory(String threadNamePrefix) {
        return new ForkJoinPool.ForkJoinWorkerThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                var thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                thread.setName(threadNamePrefix + "-" + counter.incrementAndGet());
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

    private static final class SupplierManagedBlock<T> implements ForkJoinPool.ManagedBlocker {
        private final Supplier<T> supplier;
        private T result;
        private boolean finished;

        private SupplierManagedBlock(Supplier<T> supplier) {
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

        T getResult() {
            return result;
        }
    }
}
