package pl.ciruk.whattowatch.core.score.imdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

final class ImdbFixtures {
    private ImdbFixtures() {
        throw new AssertionError();
    }

    /**
     * Gzips the fixture TSV {@code fileName} (without ".gz") into {@code directory}, the way IMDb serves it.
     */
    static Path gzipped(String fileName, Path directory) {
        var target = directory.resolve(fileName + ".gz");
        try (InputStream tsv = ImdbFixtures.class.getResourceAsStream("/imdb/" + fileName);
             var gzip = new GZIPOutputStream(Files.newOutputStream(target))) {
            tsv.transferTo(gzip);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return target;
    }
}
