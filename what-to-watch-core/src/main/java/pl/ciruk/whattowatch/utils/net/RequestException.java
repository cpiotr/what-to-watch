package pl.ciruk.whattowatch.utils.net;

/**
 * A request that failed, as opposed to a page that doesn't exist. Callers mustn't cache its absence.
 */
public class RequestException extends RuntimeException {
    RequestException(String message) {
        super(message);
    }

    RequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
