package pl.ciruk.whattowatch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.ciruk.whattowatch.boot.WhatToWatchBoot;
import pl.ciruk.whattowatch.boot.config.Bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WhatToWatchBoot.class, properties = "w2w.bootstrap.enabled=false")
class WhatToWatchBootTest {
    @Autowired
    private ApplicationContext context;

    @DynamicPropertySource
    static void emptyImdbDatasets(DynamicPropertyRegistry registry) throws IOException {
        var directory = Files.createTempDirectory("imdb");
        directory.toFile().deleteOnExit();
        writeGzipped(directory.resolve("title.ratings.tsv.gz"), "tconst\taverageRating\tnumVotes\n");
        writeGzipped(directory.resolve("title.basics.tsv.gz"), "tconst\ttitleType\tprimaryTitle\toriginalTitle\n");
        registry.add("w2w.imdb.datasets-directory", directory::toString);
    }

    private static void writeGzipped(Path file, String content) throws IOException {
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(file))) {
            gzip.write(content.getBytes(UTF_8));
        }
        file.toFile().deleteOnExit();
    }

    @Test
    void shouldNotWarmUpCachesWhenDisabled() {
        assertThat(context.getBeansOfType(Bootstrap.class)).isEmpty();
    }
}
