package pl.ciruk.whattowatch.utils.text;

public class MissingValueException extends RuntimeException {
    public MissingValueException(String msg) {
        super(msg);
    }

    public MissingValueException(Throwable cause) {
        super(cause);
    }

    public MissingValueException(Throwable cause, String msg) {
        super(msg, cause);
    }

    public MissingValueException() {

    }
}
