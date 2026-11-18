package pl.ciruk.whattowatch.boot.boundary;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.neural.MyRatings;

import java.util.List;

@RestController
@RequestMapping("/resources/ratings")
public class Ratings {
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 10;
    private static final String FILMWEB = "Filmweb";

    private final MyRatings myRatings;

    public Ratings(MyRatings myRatings) {
        this.myRatings = myRatings;
    }

    // The whole file, to copy over the bundled ratings in the repository
    @GetMapping(produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> all() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("rated-films.csv").build().toString())
                .body(myRatings.toCsv());
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rate(@RequestBody RatingRequest request) {
        if (lacksTitleOrYear(request.title(), request.originalTitle(), request.year())) {
            throw badRequest("A title or an original title, and a year, are needed");
        }
        if (request.rating() < MIN_RATING || request.rating() > MAX_RATING) {
            throw badRequest("A rating is from 1 to 10");
        }
        if (lacksFilmwebScore(request.scores())) {
            throw badRequest("A title without a Filmweb score can't be rated");
        }
        myRatings.rate(request.toRatedTitle());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String originalTitle,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "FILM") TitleKind kind) {
        if (lacksTitleOrYear(title, originalTitle, year)) {
            throw badRequest("A title or an original title, and a year, are needed");
        }
        var rated = Title.builder()
                .title(blankAsNull(title))
                .originalTitle(blankAsNull(originalTitle))
                .year(year)
                .kind(kind)
                .build();
        if (!myRatings.remove(rated)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The title has no rating");
        }
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
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
