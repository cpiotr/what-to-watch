package pl.ciruk.whattowatch.boot.boundary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeoutException;

// Every error is a Problem Details body: Spring's own errors through the base class, the rest here
@RestControllerAdvice
class ApiErrors extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ExceptionHandler(TimeoutException.class)
    ProblemDetail timedOut() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Request timed out");
    }

    // Details stay in the server log: the exception holds internal class names and scraped URLs
    @ExceptionHandler(Exception.class)
    ProblemDetail internalError(Exception exception) {
        LOGGER.error("Request failed", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
    }
}
