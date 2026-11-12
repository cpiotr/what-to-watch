package pl.ciruk.whattowatch.core.suggest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.description.DescriptionProvider;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.score.ScoresProvider;
import pl.ciruk.whattowatch.core.title.TitleKind;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.core.title.TitleProvider;
import pl.ciruk.whattowatch.utils.concurrent.CompletableFutures;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SuggestionProviderTest {

    private DescriptionProvider descriptionProvider;
    private ScoresProvider scoreProvider;
    private Cache<Title, Film> cache;
    private SuggestionProvider suggestionProvider;
    private Title title;
    private Description description;

    @BeforeEach
    void setUp() {
        TitleProvider titleProvider = mock(TitleProvider.class);
        title = createTitle();
        when(titleProvider.streamOfTitles(TitleKind.FILM, 1))
                .thenAnswer(ignored -> Stream.of(title));

        descriptionProvider = mock(DescriptionProvider.class);
        description = createDescription(title);
        when(descriptionProvider.findDescriptionBy(title)).thenReturn(Optional.of(description));

        scoreProvider = mock(ScoresProvider.class);
        when(scoreProvider.findScoresBy(description))
                .thenAnswer(ignored -> Stream.of(Score.critic(1.0, 100)));

        cache = Caffeine.newBuilder().build();
        suggestionProvider = new SuggestionProvider(
                titleProvider,
                descriptionProvider,
                List.of(scoreProvider),
                Executors.newSingleThreadExecutor(),
                cache);
    }

    @Test
    void shouldNotSearchForFilmWhenCached() {
        cache.put(title, new Film(null, null));

        List<Film> films = CompletableFutures.getAllOf(suggestionProvider.suggest(TitleKind.FILM, 1))
                .collect(toList());
        assertThat(films).isNotEmpty();

        verifyNoMoreInteractions(descriptionProvider);
        verifyNoMoreInteractions(scoreProvider);
    }

    @Test
    void shouldSearchForSeriesWhenFilmWithSameTitleIsCached() {
        cache.put(title, new Film(null, null));
        var series = Title.builder()
                .year(title.year())
                .title(title.localTitle())
                .originalTitle(title.originalTitle())
                .kind(TitleKind.SERIES)
                .build();
        TitleProvider titleProvider = mock(TitleProvider.class);
        when(titleProvider.streamOfTitles(TitleKind.SERIES, 1)).thenAnswer(ignored -> Stream.of(series));
        var provider = new SuggestionProvider(
                titleProvider,
                descriptionProvider,
                List.of(scoreProvider),
                Executors.newSingleThreadExecutor(),
                cache);

        assertThat(CompletableFutures.getAllOf(provider.suggest(TitleKind.SERIES, 1))).containsExactly(Film.empty());

        verify(descriptionProvider).findDescriptionBy(series);
    }

    @Test
    void shouldSearchForFilm() {
        List<Film> films = CompletableFutures.getAllOf(suggestionProvider.suggest(TitleKind.FILM, 1))
                .collect(toList());
        assertThat(films).isNotEmpty();

        verify(descriptionProvider).findDescriptionBy(title);
        verify(scoreProvider).findScoresBy(description);
    }

    @Test
    void shouldSearchOnceForFilmRequestedTwiceAtOnce() {
        var descriptionFound = new CountDownLatch(1);
        when(descriptionProvider.findDescriptionBy(title)).thenAnswer(ignored -> {
            descriptionFound.await();
            return Optional.of(description);
        });

        var firstRequest = suggestionProvider.suggest(TitleKind.FILM, 1).toList();
        var secondRequest = suggestionProvider.suggest(TitleKind.FILM, 1).toList();
        descriptionFound.countDown();

        assertThat(CompletableFutures.getAllOf(secondRequest)).isEqualTo(CompletableFutures.getAllOf(firstRequest).toList());
        verify(descriptionProvider).findDescriptionBy(title);
        verify(scoreProvider).findScoresBy(description);
    }

    @Test
    void shouldCacheEmptyFilmIfDescriptionIsMissing() {
        when(descriptionProvider.findDescriptionBy(title)).thenReturn(Optional.empty());

        List<Film> films = CompletableFutures.getAllOf(suggestionProvider.suggest(TitleKind.FILM, 1))
                .collect(toList());
        assertThat(films).containsExactly(Film.empty());

        assertThat(cache.asMap()).containsEntry(title, Film.empty());
    }

    @Test
    void shouldNotCacheFilmWhenDescriptionFails() {
        when(descriptionProvider.findDescriptionBy(title)).thenThrow(new IllegalStateException("Throttled"));

        List<Film> films = CompletableFutures.getAllOf(suggestionProvider.suggest(TitleKind.FILM, 1))
                .collect(toList());
        assertThat(films).containsExactly(Film.empty());

        assertThat(cache.asMap()).doesNotContainKey(title);
    }

    @Test
    void shouldKeepOtherScoresWhenOneProviderFails() {
        var failingScoreProvider = mock(ScoresProvider.class);
        when(failingScoreProvider.findScoresBy(description))
                .thenThrow(new IllegalStateException("Unexpected page"));
        var provider = createProviderWith(List.of(scoreProvider, failingScoreProvider));

        List<Film> films = CompletableFutures.getAllOf(provider.suggest(TitleKind.FILM, 1))
                .collect(toList());

        assertThat(films).hasSize(1);
        assertThat(films.get(0).scores()).containsExactly(Score.critic(1.0, 100));
        assertThat(cache.asMap()).doesNotContainKey(title);
    }

    @Test
    void shouldKeepOtherScoresWhenProviderStreamFailsLazily() {
        var lazilyFailingScoreProvider = mock(ScoresProvider.class);
        when(lazilyFailingScoreProvider.findScoresBy(description))
                .thenAnswer(ignored -> Stream.of("page").<Score>map(page -> {
                    throw new IllegalStateException("Unexpected page");
                }));
        var provider = createProviderWith(List.of(scoreProvider, lazilyFailingScoreProvider));

        List<Film> films = CompletableFutures.getAllOf(provider.suggest(TitleKind.FILM, 1))
                .collect(toList());

        assertThat(films.get(0).scores()).containsExactly(Score.critic(1.0, 100));
    }

    @Test
    void shouldCacheFilmWhenAllProvidersSucceed() {
        List<Film> films = CompletableFutures.getAllOf(suggestionProvider.suggest(TitleKind.FILM, 1))
                .collect(toList());

        assertThat(cache.asMap()).containsEntry(title, films.get(0));
    }

    private SuggestionProvider createProviderWith(List<ScoresProvider> scoresProviders) {
        TitleProvider titleProvider = mock(TitleProvider.class);
        when(titleProvider.streamOfTitles(TitleKind.FILM, 1))
                .thenAnswer(ignored -> Stream.of(title));
        return new SuggestionProvider(
                titleProvider,
                descriptionProvider,
                scoresProviders,
                Executors.newSingleThreadExecutor(),
                cache);
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
