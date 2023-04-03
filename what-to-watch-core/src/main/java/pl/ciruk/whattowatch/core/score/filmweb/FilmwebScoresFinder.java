package pl.ciruk.whattowatch.core.score.filmweb;

import org.jsoup.nodes.Element;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresFinder;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.text.NumberTokenizer;

import java.util.Optional;
import java.util.stream.Stream;

import static pl.ciruk.whattowatch.utils.stream.Predicates.not;

public class FilmwebScoresFinder implements ScoresFinder {
    private static final String FILMWEB = "Filmweb";

    private final FilmwebProxy filmwebProxy;

    public FilmwebScoresFinder(FilmwebProxy filmwebProxy) {
        this.filmwebProxy = filmwebProxy;
    }

    @Override
    public Stream<Element> searchFor(Description description) {
        var title = description.getTitle();
        var optionalResult = filmwebProxy.searchBy(title.asText(), title.year());

        return optionalResult.stream()
                .flatMap(FilmwebStreamSelectors.FILMS_FROM_SEARCH_RESULT::extractFrom);
    }

    @Override
    public boolean matchesDescription(Element scoreResult, Description description) {
        return extractTitle(scoreResult).matches(description.getTitle());
    }

    @Override
    public Optional<Score> extractScore(Element scoreResult) {
        return getDetailsAndFindScore(scoreResult);
    }

    @Override
    public String getName() {
        return FILMWEB;
    }

    private Title extractTitle(Element result) {
        var title = FilmwebSelectors.TITLE_FROM_SEARCH_RESULT.extractFrom(result).orElse("");
        var originalTitle = FilmwebSelectors.ORIGINAL_TITLE_FROM_SEARCH_RESULT.extractFrom(result).orElse("");
        var year = FilmwebSelectors.YEAR_FROM_SEARCH_RESULT.extractFrom(result)
                .filter(not(String::isEmpty))
                .map(Integer::parseInt)
                .orElse(0);
        return Title.builder()
                .title(title)
                .originalTitle(originalTitle)
                .year(year)
                .build();
    }

    private Optional<Score> getDetailsAndFindScore(Element result) {
        return FilmwebSelectors.LINK_FROM_SEARCH_RESULT.extractFrom(result)
                .flatMap(this::findScoreInDetailsPage)
                .filter(this::isPositive);
    }

    private Optional<Score> findScoreInDetailsPage(String linkToDetails) {
        return filmwebProxy.getPageWithFilmDetailsFor(linkToDetails)
                .flatMap(FilmwebSelectors.SCORE_FROM_DETAILS::extractFrom)
                .map(scoreText -> createScore(scoreText, linkToDetails));
    }

    private boolean isPositive(Score score) {
        return score.grade() > 0 && score.quantity() > 0;
    }

    private Score createScore(String scoreText, String link) {
        var numberTokenizer = new NumberTokenizer(scoreText);
        var rating = numberTokenizer.hasMoreTokens() ? numberTokenizer.nextToken().asNormalizedDouble() : -1;
        var quantity = numberTokenizer.hasMoreTokens() ? (int) numberTokenizer.nextToken().asSimpleLong() : -1;
        return Score.builder()
                .grade(rating / 10.0)
                .quantity(quantity)
                .source(FILMWEB)
                .type(ScoreType.AMATEUR)
                .url(filmwebProxy.resolveLink(link).toString())
                .build();
    }
}
