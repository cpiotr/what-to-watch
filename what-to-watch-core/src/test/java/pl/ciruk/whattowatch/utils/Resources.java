package pl.ciruk.whattowatch.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public final class Resources {
    public static String readContentOf(String resourceName) {
        try {
            var url = Resources.class.getClassLoader().getResource(resourceName);
            var uri = Objects.requireNonNull(url).toURI();
            return new String(
                    Files.readAllBytes(Paths.get(uri)),
                    Charset.defaultCharset());
        } catch (IOException | URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private Resources() {
        throw new AssertionError();
    }
}
