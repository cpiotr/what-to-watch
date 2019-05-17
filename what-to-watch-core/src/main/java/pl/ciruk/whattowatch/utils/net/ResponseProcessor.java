package pl.ciruk.whattowatch.utils.net;

import okhttp3.Response;

import java.util.function.UnaryOperator;

public interface ResponseProcessor extends UnaryOperator<Response> {
    default Response process(Response request) {
        return apply(request);
    }

    class ResponseProcessingException extends RuntimeException {
        public ResponseProcessingException(Throwable cause) {
            super(cause);
        }

        public ResponseProcessingException(String message) {
            super(message);
        }
    }

}
