package pl.ciruk.whattowatch.utils.net;

import okhttp3.Request;

import java.util.function.UnaryOperator;

public interface RequestProcessor extends UnaryOperator<Request> {
    default Request process(Request request) {
        return apply(request);
    }
}
