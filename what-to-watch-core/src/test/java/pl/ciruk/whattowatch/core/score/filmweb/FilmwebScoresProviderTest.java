package pl.ciruk.whattowatch.core.score.filmweb;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.score.Score;
import pl.ciruk.whattowatch.core.source.FilmwebFilm;
import pl.ciruk.whattowatch.core.source.FilmwebProxy;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.metrics.Names;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FilmwebScoresProviderTest {
    private static final String TITLE = "Inception";
    private static final FilmwebFilm INCEPTION = new FilmwebFilm(500_891, "Incepcja", TITLE, 2010);

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private FilmwebScoresProvider provider;

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(registry);
        var filmwebProxy = mock(FilmwebProxy.class);
        when(filmwebProxy.searchBy(anyString(), anyInt())).thenAnswer(ignored -> Stream.empty());
        when(filmwebProxy.searchBy(TITLE, 2010)).thenAnswer(ignored -> Stream.of(INCEPTION));
        when(filmwebProxy.getPageWithFilmDetailsFor(any())).thenReturn(Optional.of(Jsoup.parse(
                "<span itemprop=\"ratingValue\">8,2</span><span itemprop=\"ratingCount\">667030</span>")));
        when(filmwebProxy.linkTo(any())).thenReturn(HttpUrl.get("https://www.filmweb.pl/film/Incepcja-2010-500891"));
        provider = new FilmwebScoresProvider(filmwebProxy, Executors.newSingleThreadExecutor());
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(registry);
        registry.close();
    }

    @Test
    void shouldReadScoreFromFilmPage() {
        var scores = provider.findScoresBy(descriptionOf(TITLE, 2010)).toList();

        assertThat(scores).hasSize(1);
        assertThat(scores.get(0).grade()).isCloseTo(0.82, within(1e-9));
        assertThat(scores.get(0).quantity()).isEqualTo(667_030);
    }

    @Test
    void shouldCountOnlyMissingScores() {
        assertThat(provider.findScoresBy(descriptionOf(TITLE, 2010))).hasSize(1);
        assertThat(provider.findScoresBy(descriptionOf("Unknown", 2010))).isEmpty();

        var missingScores = registry.get(Names.getNameForMissingScores()).tag("provider", "Filmweb").gauge().value();
        assertThat(missingScores).isEqualTo(1.0);
    }

    private static Description descriptionOf(String title, int year) {
        return Description.builder()
                .title(Title.builder().title(title).originalTitle(title).year(year).build())
                .build();
    }
}
