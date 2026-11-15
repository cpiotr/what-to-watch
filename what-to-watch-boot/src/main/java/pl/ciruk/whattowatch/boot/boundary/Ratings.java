package pl.ciruk.whattowatch.boot.boundary;

import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.neural.MyRatings;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

@Named
@Path("/ratings")
public class Ratings {
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 10;
    private static final String FILMWEB = "Filmweb";

    private final MyRatings myRatings;

    @Inject
    public Ratings(MyRatings myRatings) {
        this.myRatings = myRatings;
    }

    // The whole file, to copy over the bundled ratings in the repository
    @GET
    @Produces("text/csv")
    public Response all() {
        return Response.ok(myRatings.toCsv())
                .header("Content-Disposition", "attachment; filename=\"rated-films.csv\"")
                .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rate(RatingRequest request) {
        if (request == null || lacksTitleOrYear(request.title(), request.originalTitle(), request.year())) {
            return Responses.badRequest("A title or an original title, and a year, are needed");
        }
        if (request.rating() < MIN_RATING || request.rating() > MAX_RATING) {
            return Responses.badRequest("A rating is from 1 to 10");
        }
        if (lacksFilmwebScore(request.scores())) {
            return Responses.badRequest("A title without a Filmweb score can't be rated");
        }
        myRatings.rate(request.toRatedTitle());
        return Response.noContent().build();
    }

    @DELETE
    public Response remove(
            @QueryParam("title") String title,
            @QueryParam("originalTitle") String originalTitle,
            @QueryParam("year") int year,
            @QueryParam("kind") @DefaultValue("FILM") TitleKind kind) {
        if (lacksTitleOrYear(title, originalTitle, year)) {
            return Responses.badRequest("A title or an original title, and a year, are needed");
        }
        var rated = Title.builder()
                .title(blankAsNull(title))
                .originalTitle(blankAsNull(originalTitle))
                .year(year)
                .kind(kind)
                .build();
        return myRatings.remove(rated) ? Response.noContent().build() : Response.status(NOT_FOUND).build();
    }

    // Some titles are only known by their original title. A missing year binds to 0.
    private static boolean lacksTitleOrYear(String title, String originalTitle, int year) {
        return blankAsNull(title) == null && blankAsNull(originalTitle) == null || year <= 0;
    }

    // My model learns from the ratings' Filmweb scores, so every rating keeps one
    private static boolean lacksFilmwebScore(List<Score> scores) {
        return scores == null || scores.stream().noneMatch(score -> FILMWEB.equalsIgnoreCase(score.source()));
    }

    static String blankAsNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
