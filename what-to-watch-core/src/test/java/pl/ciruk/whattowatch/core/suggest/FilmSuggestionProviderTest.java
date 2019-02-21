package pl.ciruk.whattowatch.core.suggest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FilmSuggestionProviderTest {

    private DescriptionProvider descriptionProvider;
    private ScoresProvider scoreProvider;
    private Map<Title, Film> cache;
    private FilmSuggestionProvider filmSuggestionProvider;
    private Title title;
    private Description description;

    @BeforeEach
    void setUp() {
        TitleProvider titleProvider = mock(TitleProvider.class);
        title = createTitle();
        when(titleProvider.streamOfTitles(1))
                .thenAnswer(ignored -> Stream.of(title));

        descriptionProvider = mock(DescriptionProvider.class);
        description = createDescription(title);
        when(descriptionProvider.findDescriptionOfAsync(title))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(description)));

        scoreProvider = mock(ScoresProvider.class);
        when(scoreProvider.findScoresByAsync(description))
                .thenAnswer(ignored -> CompletableFuture.completedFuture(Stream.of(Score.critic(1.0, 100))));

        cache = new ConcurrentHashMap<>();
        filmSuggestionProvider = new FilmSuggestionProvider(
                titleProvider,
                descriptionProvider,
                List.of(scoreProvider),
                Executors.newSingleThreadExecutor(),
                cache);
    }

    @Test
    void shouldNotSearchForFilmWhenCached() {
        cache.put(title, Film.builder().build());

        List<Film> films = CompletableFutures.getAllOf(filmSuggestionProvider.suggestFilms(1))
                .collect(toList());
        assertThat(films).isNotEmpty();

        verifyZeroInteractions(descriptionProvider);
        verifyZeroInteractions(scoreProvider);
    }

    @Test
    void shouldSearchForFilm() {
        List<Film> films = CompletableFutures.getAllOf(filmSuggestionProvider.suggestFilms(1))
                .collect(toList());
        assertThat(films).isNotEmpty();

        verify(descriptionProvider).findDescriptionOfAsync(title);
        verify(scoreProvider).findScoresByAsync(description);
    }

    private Description createDescription(Title title) {
        return Description.builder()
                .foundFor(title)
                .title(title)
                .build();
    }

    private Title createTitle() {
        return Title.builder()
                .year(2010)
                .title("Test title")
                .originalTitle("Test original title")
                .build();
    }
}
