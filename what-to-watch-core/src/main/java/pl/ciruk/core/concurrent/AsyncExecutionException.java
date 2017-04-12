package pl.ciruk.core.concurrent;

public class AsyncExecutionException extends RuntimeException {
    public AsyncExecutionException(Throwable e) {
        super(e);
    }
}
