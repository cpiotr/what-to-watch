package pl.ciruk.whattowatch.core.score;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.ciruk.whattowatch.core.description.Description;
import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.metrics.Names;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GenericScoreProviderTest {
    private static final String PROVIDER = "TestProvider";

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private GenericScoreProvider<String> provider;

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(registry);
        var scoresByTitle = Map.of("Found", Score.amateur(0.8, 1_000));
        provider = new GenericScoreProvider<>(Executors.newSingleThreadExecutor(), new ScoresFinder<>() {
            @Override
            public Stream<String> searchFor(Description description) {
                return Stream.of(description.titleAsText());
            }

            @Override
            public boolean matchesDescription(String searchResult, Description description) {
                return true;
            }

            @Override
            public Optional<Score> extractScore(String searchResult) {
                return Optional.ofNullable(scoresByTitle.get(searchResult));
            }

            @Override
            public String getName() {
                return PROVIDER;
            }
        });
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(registry);
        registry.close();
    }

    @Test
    void shouldCountOnlyMissingScores() {
        assertThat(provider.findScoresBy(descriptionOf("Found"))).hasSize(1);
        assertThat(provider.findScoresBy(descriptionOf("Missing"))).isEmpty();

        var missingScores = registry.get(Names.getNameForMissingScores()).tag("provider", PROVIDER).gauge().value();
        assertThat(missingScores).isEqualTo(1.0);
    }

    private static Description descriptionOf(String title) {
        return Description.builder()
                .title(Title.builder().title(title).year(2020).build())
                .build();
    }
}
