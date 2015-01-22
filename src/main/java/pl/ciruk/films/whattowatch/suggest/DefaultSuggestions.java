package pl.ciruk.films.whattowatch.suggest;

import pl.ciruk.films.whattowatch.Film;
import pl.ciruk.films.whattowatch.cache.RedisCache;
import pl.ciruk.films.whattowatch.description.DescriptionProvider;
import pl.ciruk.films.whattowatch.score.ScoresProvider;
import pl.ciruk.films.whattowatch.title.Title;
import pl.ciruk.films.whattowatch.title.TitleProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Named
public class DefaultSuggestions implements FilmSuggestionProvider {
    private TitleProvider titles;

    private DescriptionProvider descriptions;

    private List<ScoresProvider> scoresProviders;

    private RedisCache redis;

    @Inject
    public DefaultSuggestions(TitleProvider titles,
                       DescriptionProvider descriptions,
                       List<ScoresProvider> scoresProviders,
                       RedisCache redis,
                       @Named("IMDB") ScoresProvider imdbScores,
                       @Named("Metacritic") ScoresProvider metacriticScores,
                       @Named("Filmweb") ScoresProvider filmwebScores) {
        this.titles = titles;
        this.descriptions = descriptions;
        this.scoresProviders = scoresProviders;
        this.redis = redis;

        scoresProviders = new ArrayList<>();
        scoresProviders.add(imdbScores);
        scoresProviders.add(metacriticScores);
        scoresProviders.add(filmwebScores);
    }

    @Override
    public Stream<Film> suggestNumberOfFilms(int numberOfFilms) {
        Stream<Title> streamOfTitles = titles.streamOfTitles()
                .limit(numberOfFilms);

        return streamOfTitles
                .parallel()
                .map(descriptions::descriptionOf)
                .filter(Optional::isPresent)
                .map(optional -> optional.get())
                .map(description -> {
                    Film film = new Film(description);
                    scoresProviders.stream()
                            .parallel()
                            .flatMap(scoreProvider -> scoreProvider.scoresOf(description))
                            .forEach(score -> film.add(score));
                    return film;
                })
                .filter(film -> film.normalizedScore() > 0.6)
                .filter(film -> film.numberOfScores() > 1)
                .sorted((first, second) -> {
                    return second.normalizedScore().compareTo(first.normalizedScore());
                })
                .peek(f -> f.setLink(
                        titles.urlFor(f.foundFor())));
    }
}
