package pl.ciruk.whattowatch.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.suggest.Film;

import java.lang.invoke.MethodHandles;
import java.util.function.Predicate;

public class FilmByScoreFilter implements Predicate<Film> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final double minimalAcceptedScore;

    public FilmByScoreFilter(double minimalAcceptedScore) {
        this.minimalAcceptedScore = minimalAcceptedScore;
    }

    @Override
    public boolean test(Film film) {
        Double score = film.normalizedScore();
        if (Double.compare(score, minimalAcceptedScore) >= 0) {
            return true;
        }
        if (Double.compare(score / minimalAcceptedScore, 0.9) > 0) {
            LOGGER.info("Omitting {} with score {}", film.getDescription().titleAsText(), score);
        }
        return false;
    }
}
