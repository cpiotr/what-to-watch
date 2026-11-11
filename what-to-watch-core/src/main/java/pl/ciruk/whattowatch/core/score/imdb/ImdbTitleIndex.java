package pl.ciruk.whattowatch.core.score.imdb;

import pl.ciruk.whattowatch.core.title.Title;
import pl.ciruk.whattowatch.utils.text.Texts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Films from IMDb's non-commercial datasets, looked up by title and year.
 */
final class ImdbTitleIndex {
    private static final Set<String> TITLE_TYPES = Set.of("movie", "tvMovie", "video", "short");
    private static final String MISSING = "\\N";
    private static final int BUFFER_SIZE = 1 << 16;

    private final Map<String, List<ImdbTitle>> titlesByName;

    private ImdbTitleIndex(Map<String, List<ImdbTitle>> titlesByName) {
        this.titlesByName = titlesByName;
    }

    /**
     * Reads films with at least {@code minVotes} votes, under their primary and original titles.
     */
    static ImdbTitleIndex load(Path ratings, Path basics, int minVotes) throws IOException {
        var ratingsById = new HashMap<String, Rating>();
        forEachRow(ratings, row -> {
            var votes = Integer.parseInt(row[2]);
            if (votes >= minVotes) {
                ratingsById.put(row[0], new Rating(Double.parseDouble(row[1]), votes));
            }
        });

        var titlesByName = new HashMap<String, List<ImdbTitle>>();
        forEachRow(basics, row -> {
            var rating = ratingsById.get(row[0]);
            if (rating != null && TITLE_TYPES.contains(row[1]) && !MISSING.equals(row[5])) {
                var title = new ImdbTitle(row[0], Integer.parseInt(row[5]), rating.average(), rating.votes());
                add(titlesByName, row[2], title);
                add(titlesByName, row[3], title);
            }
        });
        return new ImdbTitleIndex(titlesByName);
    }

    Optional<ImdbTitle> find(Title title) {
        return Stream.of(title.originalTitle(), title.localTitle())
                .filter(Objects::nonNull)
                .map(Texts::unify)
                .filter(name -> !name.isBlank())
                .distinct()
                .flatMap(name -> titlesByName.getOrDefault(name, List.of()).stream())
                .filter(candidate -> Math.abs(candidate.year() - title.year()) <= 1)
                .max(Comparator.comparingInt(ImdbTitle::votes));
    }

    int size() {
        return titlesByName.size();
    }

    private static void add(Map<String, List<ImdbTitle>> titlesByName, String name, ImdbTitle title) {
        var titles = titlesByName.computeIfAbsent(Texts.unify(name), key -> new ArrayList<>(1));
        if (!titles.contains(title)) {
            titles.add(title);
        }
    }

    private static void forEachRow(Path file, Consumer<String[]> consumer) throws IOException {
        try (var reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(Files.newInputStream(file), BUFFER_SIZE), UTF_8),
                BUFFER_SIZE)) {
            reader.lines()
                    .skip(1)
                    .map(line -> line.split("\t", -1))
                    .forEach(consumer);
        }
    }

    record ImdbTitle(String id, int year, double rating, int votes) {
    }

    private record Rating(double average, int votes) {
    }
}
