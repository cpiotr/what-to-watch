package pl.ciruk.whattowatch.boot.boundary;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

final class Responses {
    private Responses() {
        throw new AssertionError();
    }

    static Response requestTimedOut() {
        return Response.status(SERVICE_UNAVAILABLE).entity("{\"error\":\"Request timed out\"}").build();
    }
}
