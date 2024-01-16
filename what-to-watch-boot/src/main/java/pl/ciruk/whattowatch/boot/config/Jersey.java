package pl.ciruk.whattowatch.boot.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boot.boundary.Descriptions;
import pl.ciruk.whattowatch.boot.boundary.Scores;
import pl.ciruk.whattowatch.boot.boundary.Suggestions;
import pl.ciruk.whattowatch.boot.boundary.Titles;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Configuration
@ApplicationPath("/resources")
public class Jersey extends ResourceConfig {
    public Jersey() {
        register(Descriptions.class);
        register(Scores.class);
        register(Titles.class);
        register(Suggestions.class);
        register(CorsFilter.class);
    }

    @Provider
    static class CorsFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
            responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
            responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        }
    }
}
