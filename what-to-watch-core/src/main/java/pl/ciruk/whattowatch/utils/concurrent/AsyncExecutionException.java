package pl.ciruk.whattowatch.utils.concurrent;

public class AsyncExecutionException extends RuntimeException {
    public AsyncExecutionException(Throwable throwable) {
        super(throwable);
    }
}
