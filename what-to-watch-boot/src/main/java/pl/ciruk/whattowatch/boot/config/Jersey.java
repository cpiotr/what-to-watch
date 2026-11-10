package pl.ciruk.whattowatch.boot.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.boundary.Descriptions;
import pl.ciruk.whattowatch.boot.boundary.Scores;
import pl.ciruk.whattowatch.boot.boundary.Suggestions;
import pl.ciruk.whattowatch.boot.boundary.Titles;

import jakarta.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath("/resources")
public class Jersey extends ResourceConfig {
    // Registering resources in the constructor is how Jersey's ResourceConfig is meant to be used
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public Jersey() {
        register(Descriptions.class);
        register(Scores.class);
        register(Titles.class);
        register(Suggestions.class);
    }
}
