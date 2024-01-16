package pl.ciruk.whattowatch.boot.boundary;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@SuppressWarnings("PMD.ClassNamingConventions")
final class Responses {
    private Responses() {
        throw new AssertionError();
    }

    static Response requestTimedOut() {
        return Response.status(SERVICE_UNAVAILABLE).entity("{\"error\":\"Request timed out\"}").build();
    }
}
