package pl.ciruk.whattowatch.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

@SuppressWarnings({"PMD.UseProperClassLoader", "PMD.ClassNamingConventions"})
public final class Resources {
    public static String readContentOf(String resourceName) {
        try {
            var url = Resources.class.getClassLoader().getResource(resourceName);
            var uri = Objects.requireNonNull(url).toURI();
            return Files.readString(Paths.get(uri), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private Resources() {
        throw new AssertionError();
    }
}
