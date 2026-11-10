package pl.ciruk.whattowatch.boot.boundary;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@SuppressWarnings("PMD.ClassNamingConventions")
final class Responses {
    private Responses() {
        throw new AssertionError();
    }

    static Response requestTimedOut() {
        return Response.status(SERVICE_UNAVAILABLE).entity("{\"error\":\"Request timed out\"}").build();
    }

    // Details stay in the server log: the exception holds internal class names and scraped URLs
    static Response internalError() {
        return Response.status(INTERNAL_SERVER_ERROR).entity("{\"error\":\"Internal error\"}").build();
    }
}
