package pl.ciruk.whattowatch.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;
import pl.ciruk.whattowatch.boundary.Descriptions;
import pl.ciruk.whattowatch.boundary.Scores;

import javax.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath("/")
public class Jersey extends ResourceConfig {
	public Jersey() {
		register(Descriptions.class);
		register(Scores.class);
	}
}
