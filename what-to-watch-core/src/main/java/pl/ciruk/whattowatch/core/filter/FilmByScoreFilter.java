package pl.ciruk.whattowatch.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.suggest.ScoredTitle;

import java.lang.invoke.MethodHandles;
import java.text.NumberFormat;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class FilmByScoreFilter implements Predicate<ScoredTitle> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ToDoubleFunction<ScoredTitle> scoring;
    private final double minimalAcceptedScore;

    public FilmByScoreFilter(ToDoubleFunction<ScoredTitle> scoring, double minimalAcceptedScore) {
        this.scoring = scoring;
        this.minimalAcceptedScore = minimalAcceptedScore;
    }

    @Override
    public boolean test(ScoredTitle film) {
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
