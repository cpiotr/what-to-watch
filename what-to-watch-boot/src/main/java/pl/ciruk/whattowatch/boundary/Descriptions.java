package pl.ciruk.whattowatch.boundary;

import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.description.DescriptionProvider;
import pl.ciruk.whattowatch.title.Title;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Component
@Path("/descriptions")
public class Descriptions {
    DescriptionProvider filmwebDescriptions;

    @Inject
    public Descriptions(DescriptionProvider filmwebDescriptions) {
        this.filmwebDescriptions = filmwebDescriptions;
    }

    @GET
    @Produces("application/json")
    public Response find(
            @QueryParam("title") String title,
            @QueryParam("originalTitle") String originalTitle,
            @QueryParam("year") int year
    ) {
        Optional<pl.ciruk.whattowatch.description.Description> description = filmwebDescriptions.descriptionOf(
                Title.builder()
                        .title(title)
                        .originalTitle(originalTitle)
                        .year(year)
                        .build()
        );

        Response response = description.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
        return response;
    }

}
