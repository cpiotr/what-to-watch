package pl.ciruk.whattowatch.boot.boundary;

import org.springframework.stereotype.Component;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.title.Title;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("/descriptions")
public class Descriptions {
    private DescriptionProvider filmwebDescriptions;

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
        var titleObject = Title.builder()
                .title(title)
                .originalTitle(originalTitle)
                .year(year)
                .build();
        var optionalDescription = filmwebDescriptions.findDescriptionBy(titleObject);

        return optionalDescription.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

}
