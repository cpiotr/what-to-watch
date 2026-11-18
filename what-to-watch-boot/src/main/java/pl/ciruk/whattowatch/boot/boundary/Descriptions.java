package pl.ciruk.whattowatch.boot.boundary;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;

@RestController
@RequestMapping("/resources/descriptions")
public class Descriptions {
    private final DescriptionProvider filmwebDescriptions;

    public Descriptions(DescriptionProvider filmwebDescriptions) {
        this.filmwebDescriptions = filmwebDescriptions;
    }

    @GetMapping
    public ResponseEntity<Description> find(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String originalTitle,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "FILM") TitleKind kind) {
        var titleObject = Title.builder()
                .title(title)
                .originalTitle(originalTitle)
                .year(year)
                .kind(kind)
                .build();

        return ResponseEntity.of(filmwebDescriptions.findDescriptionBy(titleObject));
    }

}
