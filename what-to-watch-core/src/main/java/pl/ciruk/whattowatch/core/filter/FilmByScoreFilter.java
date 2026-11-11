package pl.ciruk.whattowatch.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.suggest.Film;

import java.lang.invoke.MethodHandles;
import java.text.NumberFormat;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class FilmByScoreFilter implements Predicate<Film> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ToDoubleFunction<Film> scoring;
    private final double minimalAcceptedScore;

    public FilmByScoreFilter(ToDoubleFunction<Film> scoring, double minimalAcceptedScore) {
        this.scoring = scoring;
        this.minimalAcceptedScore = minimalAcceptedScore;
    }

    @Override
    public boolean test(Film film) {
        double score = scoring.applyAsDouble(film);
        if (Double.compare(score, minimalAcceptedScore) >= 0) {
            return true;
        }
        if (Double.compare(score / minimalAcceptedScore, 0.9) > 0) {
            LOGGER.info("Omitting {} with score {}", film.description().titleAsText(), NumberFormat.getNumberInstance().format(score));
        }
        return false;
    }
}
