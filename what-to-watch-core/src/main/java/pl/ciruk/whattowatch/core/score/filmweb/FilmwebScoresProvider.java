package pl.ciruk.whattowatch.core.score.filmweb;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import okhttp3.HttpUrl;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoreType;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.source.FilmwebTitle;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.utils.metrics.Names;
import pl.ciruk.whattowatch.utils.metrics.Tags;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FilmwebScoresProvider implements ScoresProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FILMWEB = "Filmweb";
    private static final int MAX_FILMWEB_SCORE = 10;

    private final FilmwebProxy filmwebProxy;
    private final Counter missingScores = Metrics.counter(
            Names.getNameForMissingScores(),
            List.of(Tags.getProviderTag(FILMWEB)));

    public FilmwebScoresProvider(FilmwebProxy filmwebProxy) {
        this.filmwebProxy = filmwebProxy;
    }

    @Override
    public Stream<Score> findScoresBy(Description description) {
        LOGGER.debug("Description: {}", description);

        var title = description.getTitle();
        var scores = filmwebProxy.searchBy(title.asText(), title.year(), title.kind())
                .filter(film -> film.toTitle().matches(title))
                .map(this::findScore)
                .flatMap(Optional::stream)
                .filter(Score::isSignificant)
                .toList();

        if (scores.isEmpty()) {
            LOGGER.warn("Missing Filmweb score for {}", description);
            missingScores.increment();
        }
        scores.forEach(score -> LOGGER.debug("Score for {}: {}", description, score));
        return scores.stream();
    }

    private Optional<Score> findScore(FilmwebTitle film) {
        return filmwebProxy.getDetailsPageFor(film)
                .flatMap(details -> scoreFrom(details, filmwebProxy.linkTo(film)));
    }

    private static Optional<Score> scoreFrom(Element details, HttpUrl link) {
        var rating = details.selectFirst("[itemprop=ratingValue]");
        var votes = details.selectFirst("[itemprop=ratingCount]");
        if (rating == null || votes == null) {
            return Optional.empty();
        }

        // Filmweb writes ratings with a decimal comma, such as "7,8"
        var grade = Double.parseDouble(rating.text().replace(',', '.')) / MAX_FILMWEB_SCORE;
        var quantity = Long.parseLong(votes.text().replaceAll("\\D", ""));
        return Optional.of(Score.builder()
                        .grade(grade)
                        .quantity(quantity)
                        .source(FILMWEB)
                        .type(ScoreType.AMATEUR)
                        .url(link.toString())
                        .build())
                .filter(score -> score.grade() > 0 && score.quantity() > 0);
    }
}
