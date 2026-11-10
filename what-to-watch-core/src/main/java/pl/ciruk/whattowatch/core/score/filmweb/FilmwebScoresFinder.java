package pl.ciruk.whattowatch.core.score.filmweb;

import okhttp3.HttpUrl;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresFinder;
import pl.ciruk.whattowatch.core.source.FilmwebFilm;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.utils.text.NumberTokenizer;

import java.util.Optional;
import java.util.stream.Stream;

public class FilmwebScoresFinder implements ScoresFinder<FilmwebFilm> {
    private static final String FILMWEB = "Filmweb";

    private final FilmwebProxy filmwebProxy;

    public FilmwebScoresFinder(FilmwebProxy filmwebProxy) {
        this.filmwebProxy = filmwebProxy;
    }

    @Override
    public Stream<FilmwebFilm> searchFor(Description description) {
        var title = description.getTitle();
        return filmwebProxy.searchBy(title.asText(), title.year());
    }

    @Override
    public boolean matchesDescription(FilmwebFilm film, Description description) {
        return film.toTitle().matches(description.getTitle());
    }

    @Override
    public Optional<Score> extractScore(FilmwebFilm film) {
        return filmwebProxy.getPageWithFilmDetailsFor(film)
                .flatMap(FilmwebSelectors.SCORE_FROM_DETAILS::extractFrom)
                .map(scoreText -> createScore(scoreText, filmwebProxy.linkTo(film)))
                .filter(this::isPositive);
    }

    @Override
    public String getName() {
        return FILMWEB;
    }

    private boolean isPositive(Score score) {
        return score.grade() > 0 && score.quantity() > 0;
    }

    private Score createScore(String scoreText, HttpUrl link) {
        var numberTokenizer = new NumberTokenizer(scoreText);
        var rating = numberTokenizer.hasMoreTokens() ? numberTokenizer.nextToken().asNormalizedDouble() : -1;
        var quantity = numberTokenizer.hasMoreTokens() ? (int) numberTokenizer.nextToken().asSimpleLong() : -1;
        return Score.builder()
                .grade(rating / 10.0)
                .quantity(quantity)
                .source(FILMWEB)
                .type(ScoreType.AMATEUR)
                .url(link.toString())
                .build();
    }
}
